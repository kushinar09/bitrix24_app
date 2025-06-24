package bitrix24.entities;

import lombok.Data;

@Data
public class ContactMulti {
    private int id;
    private String type;
    private String value;

    public ContactMulti(int id, String type, String value) {
        this.id = id;
        this.type = type;
        this.value = value;
    }
}
