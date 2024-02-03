package com.example;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;

public class Main {
	static final String CERTIFICATE_PATH = "<証明書ファイルのパス (.pfx)>";
	static final String CERTIFICATE_PASSWORD = "";
	static final String TENANT_ID = "<テナント ID>";
	static final String clientId = "<クライアント ID>";
	static final String SCOPE = "https://XXX.sharepoint.com/.default";
	static final String SITE_URL = "https://XXX.sharepoint.com/sites/YYY";
	static final String FOLDER_RELATIVE_PATH = "/sites/YYY/Shared Documents";
    static final String SOURCE_FILE_PATH = "C:\\tmp\\SPOUploadSample\\SPOUploadSample\\test.txt";
	static final String FILE_NAME = "C:\\AAA\\BBB.txt";

    public static void main(String[] args) throws Exception {
        String data = Files.readString(Paths.get(SOURCE_FILE_PATH));
        UploadFile(data);
    }

    private static ConfidentialClientApplication buildConfidentialClientObject() throws Exception {
		final KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(new FileInputStream(CERTIFICATE_PATH), CERTIFICATE_PASSWORD.toCharArray());
		final String alias = keyStore.aliases().nextElement();
		final KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, new KeyStore.PasswordProtection(CERTIFICATE_PASSWORD.toCharArray()));

        final ConfidentialClientApplication app = ConfidentialClientApplication.builder(
            clientId, 
            ClientCredentialFactory.createFromCertificate(entry.getPrivateKey(), (X509Certificate) entry.getCertificate()))
            .authority("https://login.microsoftonline.com/" + TENANT_ID)
            .build();
        return app;
    }

    private static String getAccessToken() throws Exception {
        final ClientCredentialParameters params = ClientCredentialParameters.builder(
            Collections.singleton(SCOPE))
            .build();
        final ConfidentialClientApplication app = buildConfidentialClientObject();
        final CompletableFuture<IAuthenticationResult> future = app.acquireToken(params);
        return future.get().accessToken();
    }

    private static void UploadFile(String data) throws Exception {
        final String accessToken = getAccessToken();
        final HttpClient httpClient = HttpClient.newHttpClient();
        final java.net.http.HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(SITE_URL + "/_api/web/GetFolderByServerRelativeUrl('" + FOLDER_RELATIVE_PATH + "/')/Files/Add(url='" + FILE_NAME + "',overwrite=true)"))
            .header("Authorization", "Bearer " + accessToken)
            .POST(HttpRequest.BodyPublishers.ofString(data))
            .build();
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(String.format("Result: %d", response.statusCode()));
    }
}
