package com.example;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
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
	static final String FILE_NAME = "ZZZ.txt";

    public static void main(String[] args) throws Exception {
        final String content = DownloadFile();
        System.out.println(content);
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

    private static String DownloadFile() throws Exception {
        final String accessToken = getAccessToken();
        final HttpClient httpClient = HttpClient.newHttpClient();
        final java.net.http.HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(SITE_URL + "/_api/web/GetFileByServerRelativeUrl('" + FOLDER_RELATIVE_PATH + "/" + FILE_NAME + "')/$value"))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
