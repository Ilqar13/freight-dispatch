# freight-dispatch

Yük sifarişini qəbul edən, bazadan ən optimal yük maşınını seçən və müştərinin ünvanına yönləndirən Spring Boot layihəsi (modul monolit).

## İşə salmaq
Tələblər: Java 21, Maven 3.9+, Docker.

```bash
docker compose up -d db
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
`dev` profili 8 demo maşın yükləyir. Swagger: http://localhost:8080/swagger-ui.html, nümunə sorğular: `requests.http`.

Hamısını Docker-də: `docker compose --profile app up --build`.

Testlər: `mvn test`

### Windows-da PostGIS DLL xətası (lokal PostgreSQL)
```
ERROR: could not load library "C:/Program Files/PostgreSQL/17/lib/postgis-3.dll":
       The specified module could not be found.
```
Bu, PostGIS-in özünün deyil, asılılığının çatışmamasıdır: `postgis-3.dll` → `libgeos_c.dll` → `libwinpthread-1.dll`. PostGIS paketi `libwinpthread-1.dll`-i `bin\postgisgui\` qovluğuna qoyur, `bin\`-ə qoymur, ona görə `postgres.exe` onu tapa bilmir. Düzəliş (Administrator lazımdır, skript özü UAC açır):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\fix-postgis-windows.ps1
# başqa versiya: ... -PgRoot 'C:\Program Files\PostgreSQL\16'
```

Skript DLL-i `bin\`-ə kopyalayır, servisi yenidən başladır; sonra `CREATE EXTENSION postgis` (Flyway `V1__init.sql`) işləyir.

Qeyd: quraşdırma zamanı sistem diskinə yer az olsa, PostGIS paketi natamam qurula bilər — bu halda Stack Builder ilə PostGIS-i yenidən quraşdırın. Alternativ: `docker compose up -d db` (lokal 5432 portu boş olmalıdır).

## Seçim necə işləyir
1. `TruckRepository.findNearestFree`: PostGIS ilə yükə uyğun tipli, tutumu kifayət edən boş maşınlardan təyinat nöqtəsinə ən yaxın `dispatch.candidate-limit` qədərini seçir.
2. `filter/TruckFilter` bean-ləri: sərt şərtlər (indi GPS yaşı). Yeni qayda üçün yeni `@Component`.
3. `client/RouteClient`: marşrut məsafəsi və vaxtı. `routing.provider=haversine` (defolt, təxmini) və ya `osrm` (real yol).
4. `ScoringService`: xal (aşağı = yaxşı). Normal rejimdə xərc + tutum itkisi, təcilidə gəliş vaxtı. Çəkilər `application.yml`-dədir.
5. `assign`: sətir kilidi ilə maşın `BUSY` olur, `assignment` səbəblə yazılır, `TruckAssignedEvent` yayımlanır.

## Paket strukturu (16gusta-admin-backend ilə eyni laylı üslub)
```
az.freight
├── FreightApplication
├── config/             DispatchProperties, RoutingProperties (@ConfigurationProperties)
├── controller/         TruckController, OrderController, DispatchController
├── service/            TruckService, OrderService, DispatchService, ScoringService, NotificationService
├── repository/         TruckRepository (PostGIS sorğusu), OrderRepository, AssignmentRepository
├── entity/             Truck, Order, Assignment
├── enums/              TruckType, TruckStatus, OrderStatus, CargoType
├── dto/                ApiResponse, TruckDto, OrderDto, AssignmentDto, AssignRequest, PositionUpdateDto, Candidate, RouteInfo
├── mapper/             TruckMapper, OrderMapper, AssignmentMapper (əllə yazılmış @Component)
├── client/             RouteClient, HaversineRouteClient, OsrmRouteClient
├── filter/             TruckFilter, PositionFreshnessFilter (sərt şərtlər)
├── event/              TruckAssignedEvent
├── exception/          ResourceNotFoundException, ApiException
├── exceptionHandler/   GlobalExceptionHandler (hamısı ApiResponse qaytarır)
├── factory/            PageRequestFactory (page/size/sort icazə siyahısı ilə)
└── util/               GeoUtils
```
Test paketləri də eyni qaydadadır: `serviceTest/`, `utilTest/`.

## API (gusta ilə eyni cavab formatı)
Bütün cavablar `ApiResponse` ilə gəlir: `{ "success": true, "message": "...", "data": ..., "page", "size", "total" }`.
Xəta: `{ "success": false, "message": "..." }`.

| Method | Path | Təsvir |
|--------|------|--------|
| GET | `/api/truck?status=&page=0&size=50&sort=id` | Maşın siyahısı (səhifələnmiş) |
| GET | `/api/truck/{id}` | Bir maşın |
| POST | `/api/truck` | Yeni maşın |
| PUT | `/api/truck/{id}/position` | GPS yeniləməsi |
| GET | `/api/order?page=0&size=50&sort=createdAt,desc` | Sifariş siyahısı |
| GET | `/api/order/{id}` | Bir sifariş |
| POST | `/api/order` | Yeni sifariş |
| POST | `/api/order/{id}/dispatch` | Namizəd maşınlar (heç nə dəyişmir) |
| POST | `/api/order/{id}/assign` | Təyinat (body boş = avtomatik, `{"truckId": 3}` = operator seçimi, başlıq `X-User`) |

Əvvəlki `/api/v1/trucks` və `/api/v1/orders` yolları `/api/truck` və `/api/order` ilə əvəz olunub.

## Növbəti addımlar
- Spring Security + Keycloak (rollar: operator, dispetçer, sürücü, müştəri)
- Müştəri, sürücü, location (stansiya/depo) cədvəlləri; geokodlama
- OSRM Table API ilə bütün namizədlər üçün bir sorğu
- Testcontainers (PostGIS) ilə inteqrasiya testləri
- GPS qəbulu üçün Redis/RabbitMQ, bildirişlər (SMS, WhatsApp, Telegram)
- Flyway ilə yeni miqrasiyalar: sürücü, sənəd, tarif, hesab-faktura

## Sürücü portalı və texniki problem axını

**Giriş:** `POST /api/driver/login` (telefon + PİN) → token. Sonrakı sorğular `X-Driver-Token` başlığı ilə gedir
(`/api/driver/**`). Token bazada yalnız SHA-256 hash kimi saxlanır, 5 uğursuz cəhddən sonra hesab 5 dəqiqə bloklanır.
Sürücünü dispetçer yaradır: `POST /api/drivers` (PİN-i sıfırlamaq: `PUT /api/drivers/{id}/pin`).
`dev` profilində hər maşına test sürücü yaranır (`+994500000001`, ... / PİN `1234`).

**Sürücü əməliyyatları:** `GET /api/driver/orders`, `/orders/available`, `POST /orders/{id}/accept | start | deliver | release | breakdown | pickup`,
`POST /api/driver/truck/repaired`.

**Texniki problem (breakdown):**
1. Sürücü `breakdown` bildirir → maşın `MAINTENANCE`, sifariş `BREAKDOWN`, nasazlıq (`incident`) yaranır.
2. Sistem nasazlıq nöqtəsinə ən yaxın BOŞ və uyğun maşını seçir (PostGIS KNN, `FOR UPDATE SKIP LOCKED`, xal = nasazlıq nöqtəsinə çatma vaxtı).
   Təyinat `REPLACEMENT` növündədir: maşın əvvəl nasazlıq nöqtəsinə, sonra təyinata gedir.
3. Boş maşın yoxdursa nasazlıq `AWAITING_TRUCK` qalır, planlaşdırıcı hər `dispatch.replacement-retry-seconds` (30) saniyədə yenidən axtarır.
   Dispetçer `POST /api/incident/{id}/assign` ilə əllə də seçə bilər.
4. Əvəzedici sürücü yükü götürəndə `pickup` edir → nasazlıq `CLOSED`, sonra adi qaydada `start` / `deliver`.
5. Əvəzedici maşın da nasaz olsa, yük hələ götürülməyibsə axtarış ilk nasazlıq nöqtəsindən davam edir.

Sifariş tarixçəsi serverdə saxlanır: `GET /api/order/{id}/events`. Miqrasiya: `V101__driver_portal.sql`.

> Qeyd: bu mühitdə Maven repozitoriyalarına çıxış olmadığına görə Java kodu kompilyasiya/test edilməyib.
> SQL miqrasiyası və KNN sorğusu real PostGIS-də yoxlanıb. İlk işə salanda `mvn clean verify` edin.

## Sifarişin tam məlumatı (V102)

Sifariş indi real logistika məlumatını saxlayır: sifarişçi (VÖEN, əlaqə), **yükləmə nöqtəsi** (ünvan, koordinat, kontakt, vaxt pəncərəsi),
çatdırılma (ünvan, qəbul edən şəxs, deadline), yük (təsvir, həcm, qablaşdırma, dəyər, ADR/UN nömrəsi, temperatur, kövrək/yığılmaz),
xidmətlər (yükləmə/boşaltma köməyi, sığorta, sürücü üçün qeydlər), ödəniş (qiymət, valyuta, üsul, kim ödəyir, qaimə) və
sifariş nömrəsi (`166-2026-00005`). Köhnə sifarişlər qorunur (yeni sütunlar boş ola bilər, nömrələri avtomatik verilir).

Dispetç məntiqi: yükləmə nöqtəsi verilibsə ən yaxın boş maşın **yükləmə nöqtəsinə** görə seçilir, məsafə və xərc
maşın → yükləmə → çatdırılma üzrə hesablanır. Yükləmə nöqtəsi yoxdursa köhnə qayda (birbaşa çatdırılma ünvanı) işləyir.
Yükləməyə gedərkən maşın nasaz olsa, əvəzedici maşın yükləmə nöqtəsinə çağırılır.
Backend yoxlamaları: koordinatlar cüt olmalıdır, vaxt pəncərələri ardıcıl olmalıdır, ADR üçün UN nömrəsi və dondurulmuş qida üçün temperatur məcburidir.

## Reyestr: maşın pasportu, sürücü və müştəri qeydiyyatı (V103)

**Maşın** (`/api/truck`, `PUT /api/truck/{id}`): tip (tentli, soyuducu, sistern, konteyner, platforma, furqon, özüboşaldan),
yükgötürmə (ton), marka/model/il, VIN, kuzov ölçüləri və həcm (m³), ox sayı, yanacaq, mülkiyyət, hidravlik rampa,
ADR, temperatur diapazonu, GPS cihaz, sığorta və texniki baxış bitmə tarixi, son servis, yürüş.

**Dispetç süzgəcləri** (sərt qaydalar): tutum, tip, `DocumentsValidFilter` (müddəti bitmiş sığorta/baxış),
`CargoRequirementsFilter` (ADR, həcm, temperatur), `DriverReadyFilter` (aktiv sürücü, etibarlı vəsiqə və tibbi arayış,
ADR yükü üçün ADR sürücü). Sürücü şərtini söndürmək üçün: `dispatch.require-driver: false`.

**Sürücü** (`/api/drivers`): FİN, doğum tarixi, vəsiqə nömrəsi/kateqoriyaları/bitmə tarixi (yük üçün C, CE, C1 və ya D),
təcrübə, tibbi arayış, ADR, təcili əlaqə, maşın bağlantısı. Yaş ≥ 21.

**Müştəri**: 
- Portal (`/api/customer/**`, `X-Customer-Token`): `register` (status PENDING), `login`, `me`, `orders`, `orders/{id}/cancel`.
  Sifariş yalnız ACTIVE müştəri verə bilər; ləğv yalnız maşın təyin olunmamış (CREATED) sifarişdə.
- Dispetçer (`/api/customers`): siyahı (status filtri), təsdiq/blok, müqavilə, ödəniş müddəti, kredit limiti, parol sıfırlama.
- Sifariş `customerId` ilə müştəriyə bağlanır.

## Müştəri köməkçisi (Claude)

Müştəri portalındakı "Köməkçi" tabı: müştəri sifarişi sərbəst mətnlə yazır, köməkçi çatışmayan sahələri soruşur,
xülasəni təsdiqlədəndən sonra `create_order` aləti ilə sifarişi sistemə göndərir. Həmçinin sifariş statusunu deyir və
maşın təyin olunmamış sifarişi ləğv edir. Alətlər yalnız daxil olmuş müştərinin adından, adi servislərlə (eyni yoxlamalar) işləyir.

İşə salmaq: Claude üçün `ANTHROPIC_API_KEY`; DeepSeek üçün `ASSISTANT_PROVIDER=deepseek` və `DEEPSEEK_API_KEY`
(açarı koda yazmayın). Model: `ASSISTANT_MODEL` (default `claude-sonnet-5-5` / `deepseek-chat`). Endpoint: `POST /api/customer/assistant`, dəqiqədə ən çox 12 mesaj.
Dəstəklənən şəhərlər `AssistantService.CITIES`-dədir; ünvan şəhər mərkəzinə bağlanır, dəqiq koordinatı dispetçer dəqiqləşdirir.
