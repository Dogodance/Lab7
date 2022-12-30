import java.net.MalformedURLException;
import java.net.URL;

public class URLDepthPair {
    private String url; // url страницы
    private int depth; // Её глубина
    public static final String URL_PREFIX = "http://"; // Префикс
    // Максимальная доступная глубина(чтобы не искать вечно, если что-то пойдет не так)
    public static final int MAX_DEPTH_VALUE = 100;
    public URLDepthPair(String url, int depth) throws MalformedURLException {
        // Если глубина вышла за пределы то выведем ошибку
        if (depth < 0 || depth > MAX_DEPTH_VALUE) {
            throw new IllegalArgumentException("Error limits of depth");
        }
        this.url = url; // Иначе примем url
        this.depth = depth; // И примем глубину
    }

    public String toString() {
        return "Глубина: " + this.depth + ", URL: " + this.url; // Вывод данных о посещенной странице
    }
    // Получение параметров
    // Получение хоста
    public String getHostName() {
        try {
            URL url = new URL(this.url);
            return url.getHost();
        }
        // Если имя хоста не может сформироваться, пробросим ошибку
        catch (MalformedURLException e) {
            System.err.println("MalformedURLException: " + e.getMessage());
            return null;
        }
    }
    // Получение пути до страницы
    public String getPagePath() {
        try {
            URL url = new URL(this.url);
            return url.getPath();
        }
        // Если путь не может сформироваться, пробросим ошибку
        catch (MalformedURLException e) {
            System.err.println("MalformedURLException: " + e.getMessage());
            return null;
        }
    }
    // Получение URL страницы
    public String getURL() {
        return this.url;
    }

    // Получение глубины
    public int getDepth() {
        return this.depth;
    }
}