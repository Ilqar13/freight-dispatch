package az.freight.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** Söhbət tarixçəsi brauzerdə saxlanır, hər sorğu ilə tam göndərilir (server vəziyyət saxlamır). */
@Data
public class AssistantRequest {

    @Valid
    @NotEmpty
    @Size(max = 30)
    private List<Msg> messages;

    @Data
    public static class Msg {
        @Pattern(regexp = "user|assistant")
        private String role;
        @NotBlank
        @Size(max = 2000)
        private String content;
    }
}
