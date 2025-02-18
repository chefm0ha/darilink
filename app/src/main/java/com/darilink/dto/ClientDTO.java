package com.darilink.dto;

import com.darilink.models.Client;

public class ClientDTO extends UserDTO {
    public static ClientDTO fromClient(Client client) {
        return fromUser(client, ClientDTO.class);
    }
}
