package az.freight.enums;

/** Texniki problemin növü (sürücü seçir). */
public enum IncidentType {
    ENGINE("Mühərrik nasazlığı"),
    TIRES("Təkər / şin problemi"),
    BRAKES("Əyləc nasazlığı"),
    ELECTRICAL("Elektrik nasazlığı"),
    COOLING("Soyutma / temperatur sistemi"),
    FUEL("Yanacaq çatışmazlığı / sızma"),
    ACCIDENT("Qəza"),
    OTHER("Digər");

    private final String label;

    IncidentType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
