package bitrix24.services;

import bitrix24.entities.BitrixOAuthResponse;
import bitrix24.entities.ContactMulti;
import bitrix24.entities.ContactResponse;
import lombok.extern.log4j.Log4j2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Service
@Log4j2
public class ContactService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final TokenStorageService tokenStorageService;
    private final TokenService tokenService;

    public ContactService(TokenStorageService tokenStorageService,
            TokenService tokenService) {
        this.tokenStorageService = tokenStorageService;
        this.tokenService = tokenService;
    }

    private String apiUrl(String method) {
        BitrixOAuthResponse token = tokenStorageService.loadToken();
        return token.getDomain() + "/rest/" + method + "?auth=" + token.getAccess_token();
    }

    private JsonNode callForJson(String method, String url, Map<String, Object> body) {
        return callForJson(method, url, body, false);
    }

    private JsonNode callForJson(String method, String url, Map<String, Object> body, boolean isRetry) {
        try {
            return callWithMethod(method, url, body);
        } catch (HttpClientErrorException.Unauthorized e) {
            if (isRetry) {
                throw new RuntimeException("Authentication failed after token refresh", e);
            }
            tokenService.refreshToken();
            String newUrl = url.replaceAll("auth=[^&]*", "auth=" + tokenStorageService.loadToken().getAccess_token());
            return callForJson(method, newUrl, body, true);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("API call failed", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during API call", e);
        }
    }

    private JsonNode callWithMethod(String method, String url, Map<String, Object> body) {
        switch (method.toLowerCase()) {
            case "get":
                return restTemplate.getForEntity(url, JsonNode.class).getBody();
            case "post":
                return restTemplate.postForEntity(url, body, JsonNode.class).getBody();
            case "delete":
                restTemplate.delete(url);
                return null;
            default:
                throw new IllegalArgumentException("Unsupported method: " + method);
        }
    }

    public List<ContactResponse> getAllContacts() throws IOException {
        Map<String, Object> payload = Map.of(
                "select", List.of("ID", "NAME", "LAST_NAME", "PHONE", "EMAIL", "WEB"));

        JsonNode response = callForJson("post", apiUrl("crm.contact.list"), payload);
        List<ContactResponse> results = new ArrayList<>();

        for (JsonNode item : response.get("result")) {
            ContactResponse contact = new ContactResponse();
            contact.setId(item.get("ID").asLong());
            contact.setFname(item.get("NAME").asText(""));
            contact.setLname(item.get("LAST_NAME").asText(""));
            contact.setPhones(parseMultiField(item.get("PHONE")));
            contact.setEmails(parseMultiField(item.get("EMAIL")));
            contact.setWebsites(parseMultiField(item.get("WEB")));
            results.add(contact);
        }

        for (ContactResponse contact : results) {
            loadAddress(contact);
            loadBankDetails(contact);
        }

        return results;
    }

    public ContactResponse getContactById(Long id) throws IOException {
        String url = apiUrl("crm.contact.get") + "&id=" + id;
        JsonNode response = callForJson("get", url, null);
        JsonNode item = response.get("result");

        ContactResponse contact = new ContactResponse();
        contact.setId(item.get("ID").asLong());
        contact.setFname(item.get("NAME").asText(""));
        contact.setLname(item.get("LAST_NAME").asText(""));
        contact.setPhones(parseMultiField(item.get("PHONE")));
        contact.setEmails(parseMultiField(item.get("EMAIL")));
        contact.setWebsites(parseMultiField(item.get("WEB")));

        loadAddress(contact);
        loadBankDetails(contact);

        return contact;
    }

    public ContactResponse createContact(ContactResponse contact) throws IOException {
        Map<String, Object> fields = new HashMap<>();
        fields.put("NAME", contact.getFname());
        fields.put("LAST_NAME", contact.getLname());
        fields.put("PHONE", toMultiField(contact.getPhones(), "PHONE"));
        fields.put("EMAIL", toMultiField(contact.getEmails(), "EMAIL"));
        fields.put("WEB", toMultiField(contact.getWebsites(), "WEB"));

        Map<String, Object> body = Map.of("fields", fields);
        String url = apiUrl("crm.contact.add");
        JsonNode response = callForJson("post", url, body);
        Long contactId = response.get("result").asLong();
        contact.setId(contactId);

        updateAddress(contact);
        updateBankDetails(contact);

        return contact;
    }

    public ContactResponse updateContact(ContactResponse contact,
            List<Integer> deleteEmails,
            List<Integer> deletePhones,
            List<Integer> deleteWebsites) throws IOException {
        Map<String, Object> fields = new HashMap<>();
        fields.put("NAME", contact.getFname());
        fields.put("LAST_NAME", contact.getLname());
        fields.put("PHONE", mergeMultiFields(deletePhones, contact.getPhones(), "PHONE"));
        fields.put("EMAIL", mergeMultiFields(deleteEmails, contact.getEmails(), "EMAIL"));
        fields.put("WEB", mergeMultiFields(deleteWebsites, contact.getWebsites(), "WEB"));

        Map<String, Object> body = Map.of("id", contact.getId(), "fields", fields);
        String url = apiUrl("crm.contact.update");
        callForJson("post", url, body);

        updateAddress(contact);
        updateBankDetails(contact);

        return contact;
    }

    private List<ContactMulti> parseMultiField(JsonNode array) {
        List<ContactMulti> list = new ArrayList<>();
        if (array != null && array.isArray()) {
            for (JsonNode item : array) {
                ContactMulti cm = new ContactMulti(
                        item.has("ID") ? item.path("ID").asInt() : 0,
                        item.path("VALUE_TYPE").asText(),
                        item.path("VALUE").asText());
                list.add(cm);
            }
        }

        return list;
    }

    private List<Map<String, Object>> toMultiField(List<ContactMulti> list, String typeId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ContactMulti item : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("TYPE_ID", typeId);
            map.put("VALUE_TYPE", item.getType());
            map.put("VALUE", item.getValue());
            result.add(map);
        }

        return result;
    }

    private List<Map<String, Object>> mergeMultiFields(List<Integer> deleteList, List<ContactMulti> newList,
            String typeId) {
        List<Map<String, Object>> result = new ArrayList<>();

        // Mark deletions
        for (Integer i : deleteList) {
            result.add(Map.of("ID", i, "DELETE", "Y"));
        }

        // Add or update new values
        for (ContactMulti item : newList) {
            Map<String, Object> map = new HashMap<>();
            if (item.getId() > 0) {
                map.put("ID", item.getId());
            }
            map.put("TYPE_ID", typeId);
            map.put("VALUE_TYPE", item.getType());
            map.put("VALUE", item.getValue());
            result.add(map);
        }

        return result;
    }

    private void loadAddress(ContactResponse contact) throws IOException {
        String url = apiUrl("crm.address.list") + "&filter[ENTITY_TYPE_ID]=3&filter[ANCHOR_ID]=" + contact.getId();
        JsonNode result = callForJson("get", url, null).path("result");

        if (result.isArray() && result.size() > 0) {
            JsonNode addr = result.get(0);
            contact.setAddressId(addr.path("ENTITY_ID").asInt(-1));
            contact.setAddressStreet(addr.path("ADDRESS_1").asText(""));
            contact.setAddressCity(addr.path("CITY").asText(""));
            String region = addr.path("REGION").asText("");
            contact.setAddressRegion(region);
        } else {
            contact.setAddressId(-1);
            contact.setAddressStreet("");
            contact.setAddressCity("");
            contact.setAddressRegion("");
        }
    }

    private void updateAddress(ContactResponse contact) {
        Map<String, Object> fields = new HashMap<>();

        // if addressId is -1, it means we need to create a new requisite
        if (contact.getAddressId() == -1) {
            // Create requisite if it doesn't exist
            fields.put("ENTITY_TYPE_ID", 3); // 3 is for Contacts
            fields.put("ENTITY_ID", contact.getId());
            fields.put("PRESET_ID", 3); // 3 is for Individual
            fields.put("NAME", "Bank detail");
            Map<String, Object> body = Map.of("fields", fields);
            String createUrl = apiUrl("crm.requisite.add");
            long requisiteId = callForJson("post", createUrl, body).get("result").asLong();

            fields.clear();

            fields.put("TYPE_ID", 11);
            fields.put("ENTITY_TYPE_ID", 8); // 8 = Requisite
            fields.put("ENTITY_ID", requisiteId);
            fields.put("ADDRESS_1", contact.getAddressStreet());
            fields.put("CITY", contact.getAddressCity());
            fields.put("REGION", contact.getAddressRegion());
            body = Map.of("fields", fields);

            callForJson("post", apiUrl("crm.address.add"), body);

        } else {
            fields.put("TYPE_ID", 11);
            fields.put("ENTITY_TYPE_ID", 8); // 8 = Requisite
            fields.put("ENTITY_ID", contact.getAddressId());
            fields.put("ADDRESS_1", contact.getAddressStreet());
            fields.put("CITY", contact.getAddressCity());

            Map<String, Object> body = Map.of("fields", fields);

            // Make sure callForJson sends proper JSON headers
            callForJson("post", apiUrl("crm.address.update"), body);
        }

    }

    public void deleteContact(Long id) {
        String url = apiUrl("crm.contact.delete") + "&id=" + id;
        callForJson("get", url, null);
    }

    private void loadBankDetails(ContactResponse contact) {
        JsonNode requisite = callForJson("post", apiUrl("crm.requisite.list"), Map.of(
                "filter", Map.of("ENTITY_TYPE_ID", 3, "ENTITY_ID", contact.getId()))).get("result");

        if (!requisite.isArray() || requisite.size() == 0)
            return;
        long reqId = requisite.get(0).path("ID").asLong();

        JsonNode bank = callForJson("post", apiUrl("crm.requisite.bankdetail.list"), Map.of(
                "filter", Map.of("ENTITY_ID", reqId))).get("result");

        if (bank.isArray() && bank.size() > 0) {
            JsonNode b = bank.get(0);
            contact.setBankId(b.path("ID").asText(""));
            contact.setBankName(b.path("RQ_BANK_NAME").asText(""));
            contact.setBankAccount(b.path("RQ_ACC_NUM").asText(""));
        }
    }

    private void updateBankDetails(ContactResponse contact) {
        // Get existing requisite by contact ID
        JsonNode requisites = callForJson("post", apiUrl("crm.requisite.list"), Map.of(
                "filter", Map.of("ENTITY_TYPE_ID", 3, "ENTITY_ID", contact.getId()))).get("result");

        Long requisiteId;
        if (requisites != null && requisites.isArray() && requisites.size() > 0) {
            requisiteId = requisites.get(0).get("ID").asLong();
        } else {
            // Create requisite if it doesn't exist
            Map<String, Object> fields = new HashMap<>();
            fields.put("ENTITY_TYPE_ID", 3); // 3 is for Contacts
            fields.put("ENTITY_ID", contact.getId());
            fields.put("PRESET_ID", 3); // 3 is for Individual
            fields.put("NAME", "Bank detail");
            Map<String, Object> body = Map.of("fields", fields);
            String createUrl = apiUrl("crm.requisite.add");
            requisiteId = callForJson("post", createUrl, body).get("result").asLong();
        }

        JsonNode bank = callForJson("post", apiUrl("crm.requisite.bankdetail.list"), Map.of(
                "filter", Map.of("ENTITY_ID", requisiteId))).get("result");

        if (bank.isArray() && bank.size() > 0) {
            JsonNode b = bank.get(0);
            int bankId = b.path("ID").asInt(0);

            Map<String, Object> fields = new HashMap<>();
            fields.put("ENTITY_ID", requisiteId);
            fields.put("RQ_BANK_NAME", contact.getBankName());
            fields.put("RQ_ACC_NUM", contact.getBankAccount());

            Map<String, Object> body = new HashMap<>();
            body.put("id", bankId);
            body.put("fields", fields);

            // Step 5: Call update endpoint
            String updateUrl = apiUrl("crm.requisite.bankdetail.update");
            callForJson("post", updateUrl, body);
        } else {
            // Create new bank detail if it doesn't exist
            Map<String, Object> fields = new HashMap<>();
            fields.put("ENTITY_ID", requisiteId);
            fields.put("NAME", "Bank detail");
            fields.put("RQ_BANK_NAME", contact.getBankName());
            fields.put("RQ_ACC_NUM", contact.getBankAccount());

            Map<String, Object> body = Map.of("fields", fields);
            String createUrl = apiUrl("crm.requisite.bankdetail.add");
            callForJson("post", createUrl, body);
        }

    }

}
