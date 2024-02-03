using Microsoft.Identity.Client;
using System.Net.Http.Headers;
using System.Security.Cryptography.X509Certificates;
using System.Text;

const string CertificatePath = @"<証明書ファイルのパス (.pfx)>";
const string? CertificatePassword = null;
const string TenantId = "<テナント ID>";
const string ClientId = "<クライアント ID>";
const string Scope = "https://XXX.sharepoint.com/.default";
const string SiteUrl = "https://XXX.sharepoint.com/sites/YYY";
const string FolderRelativeUrl = "/sites/YYY/Shared Documents";
const string SourceFilePath = @"C:\AAA\BBB.txt";
const string FileName = "ZZZ.txt";

static X509Certificate2 GetCertificate()
{
    var collection = new X509Certificate2Collection();
    collection.Import(CertificatePath, CertificatePassword, X509KeyStorageFlags.PersistKeySet);
    var certificate = collection[0];
    return certificate;
}

static async Task<string> GetAccessToken()
{
    var certificate = GetCertificate();
    var app = ConfidentialClientApplicationBuilder.Create(ClientId)
        .WithCertificate(certificate)
        .Build();
    var scopes = new[] { Scope };
    var authResult = await app.AcquireTokenForClient(scopes: scopes)
         .WithTenantId(TenantId)
         .ExecuteAsync();
    return authResult.AccessToken;
}

static async Task UploadFile(string data)
{
    var accessToken = await GetAccessToken();
    using var client = new HttpClient();
    using var content = new StringContent(data, Encoding.UTF8, "text/plain");

    client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
    var requestUri = $"{SiteUrl}/_api/web/GetFolderByServerRelativeUrl('{FolderRelativeUrl}/')/Files/Add(url='{FileName}', overwrite=true)";
    using var response = await client.PostAsync(requestUri, content);
    response.EnsureSuccessStatusCode();

    Console.WriteLine("Success");
}

var data = await File.ReadAllTextAsync(SourceFilePath);
await UploadFile(data);
