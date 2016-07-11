package net.hawkengine.agent.services;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import net.hawkengine.agent.AgentConfiguration;
import net.hawkengine.agent.models.FetchMaterialTask;

import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

public class TFSMaterialService extends MaterialService {

    @Override
    public String fetchMaterial(FetchMaterialTask task) {
        String errorMessage;

        HTTPBasicAuthFilter credentials = this.handleCredentials(task.getMaterialSpecificDetails());
        if (credentials != null) {
            super.restClient.addFilter(credentials);
        }

        String resource = this.constructResource(task.getMaterialSpecificDetails());
        WebResource webResource = super.restClient.resource(resource);
        ClientResponse response = webResource.accept("application/zip").get(ClientResponse.class);

        int responseCode = response.getStatus();
        if (responseCode == 200) {

            String filePath = Paths.get(AgentConfiguration.getInstallInfo().getAgentTempDirectoryPath(), UUID.randomUUID() + ".zip").toString();
            errorMessage = super.fileManagementService.streamToFile(response.getEntityInputStream(), filePath);
            if (errorMessage != null) {
                return errorMessage;
            }

            String destination = Paths.get(AgentConfiguration.getInstallInfo().getAgentPipelinesDir(), task.getPipelineName(), task.getDestination()).toString();
            errorMessage = super.fileManagementService.unzipFile(filePath, destination);
            super.fileManagementService.deleteFile(filePath);
            if (errorMessage != null) {
                return errorMessage;
            }
        } else {
            errorMessage = this.generateErrorMessage(responseCode);
        }

        return errorMessage;
    }

    private HTTPBasicAuthFilter handleCredentials(Map<String, Object> materialSpecificDetails) {

        HTTPBasicAuthFilter credentials = null;

        if (materialSpecificDetails.containsKey("username") && materialSpecificDetails.containsKey("password")) {
            String username = materialSpecificDetails.get("username").toString();
            String password = super.securityService.decrypt(materialSpecificDetails.get("password").toString());

            credentials = new HTTPBasicAuthFilter(username, password);
        }

        return credentials;
    }

    private String constructResource(Map<String, Object> materialSpecificDetails) {

        String resource = null;

        if (materialSpecificDetails.containsKey("domain") && materialSpecificDetails.containsKey("projectPath")) {
            String instance = materialSpecificDetails.get("domain").toString();
            String path = materialSpecificDetails.get("projectPath").toString();

            resource = String.format(
                    "https://%s.visualstudio.com/defaultcollection/_apis/tfvc/items/%s&api-version=1.0",
                    instance,
                    path);
        }

        return resource;
    }

    private String generateErrorMessage(int responseCode) {
        switch (responseCode) {
            case 203:
            case 401:
                return "Invalid credentials.";
            case 404:
                return "Item not found.";
            default:
                return "Unknown error has occurred.";
        }
    }
}