package bitrix24.entities;

import java.util.List;

import lombok.Data;

@Data
public class ContactResponse {
    private Long id;
    private String fname;
    private String lname;
    private List<ContactMulti> phones;
    private List<ContactMulti> emails;
    private List<ContactMulti> websites;

    private int addressId;
    private String addressStreet;
    private String addressRegion;
    private String addressCity;

    private String bankId;
    private String bankName;
    private String bankAccount;
}



