package dev.tenacity.utils.misc;

import dev.tenacity.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class NetworkingUtils implements Utils {

    private static final String USER_AGENT = "KingClient";
    public static boolean bypassed;


    public static HttpResponse httpsConnection(String url) {
        bypassSSL();
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.connect();
            return new HttpResponse(FileUtils.readInputStream(connection.getInputStream()), connection.getResponseCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HttpResponse httpConnection(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.connect();
            return new HttpResponse(FileUtils.readInputStream(connection.getErrorStream()), connection.getResponseCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static int image = 0;
    public static ResourceLocation downloadImage(String url) {
        final ThreadDownloadImageData avatarImage = new ThreadDownloadImageData(null, url, null,
                new IImageBuffer() {
                    @Override
                    public BufferedImage parseUserSkin(BufferedImage image) {
                        return image;
                    }

                    @Override
                    public void skinAvailable() {
                    }
                });

        ResourceLocation location = new ResourceLocation("onlineAsset/" + (image++));
        mc.getTextureManager().loadTexture(location, avatarImage);
        return location;
    }

    public static void bypassSSL() {
        if (!bypassed) {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(
                                X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(
                                X509Certificate[] certs, String authType) {
                        }
                    }
            };
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception ignored) {
            }
            bypassed = true;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class HttpResponse {
        public String content;
        public int response;
    }
}
