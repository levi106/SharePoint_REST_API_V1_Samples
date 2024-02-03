using Microsoft.Identity.Client;
using System.Net.Http.Headers;
using System.Security.Cryptography.X509Certificates;

const string CertificatePath = @"<証明書ファイルのパス (.pfx)>";
const string? CertificatePassword = null;
const string TenantId = "<テナント ID>";
const string ClientId = "<クライアント ID>";
const string Scope = "https://XXX.sharepoint.com/.default";
const string SiteUrl = "https://XXX.sharepoint.com/sites/YYY";
const string FolderRelativeUrl = "/sites/YYY/Shared Documents";
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

static async Task<string> DownloadFile()
{
    var accessToken = await GetAccessToken();
    using var client = new HttpClient();

    client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
    var requestUri = $"{SiteUrl}/_api/web/GetFileByServerRelativeUrl('{FolderRelativeUrl}/{FileName}')/$value";
    using var response = await client.GetAsync(requestUri);
    response.EnsureSuccessStatusCode();

    return await response.Content.ReadAsStringAsync();
}

var content = await DownloadFile();
Console.WriteLine(content);
