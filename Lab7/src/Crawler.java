import java.lang.Exception;
import java.net.MalformedURLException;
import java.net.*;
import java.io.*;
import java.util.LinkedList;

public class Crawler {
    public static final int HTTP_PORT = 80; // Порт подключения
    public static final String HOOK_REF = "<a href=\""; // Адрес рука
    public static final String BAD_REQUEST_LINE = "HTTP/1.1 400 Bad Request"; // Header HTTP файла
    private LinkedList<URLDepthPair> notVisitedList; // Список не посещенных адресов
    private LinkedList<URLDepthPair> visitedList; // Список посещенных адресов
    int depth;    // Глубина поиска
    // Конструктор
    public Crawler() {
        notVisitedList = new LinkedList<URLDepthPair>(); // Создание пустого списка
        visitedList = new LinkedList<URLDepthPair>(); // Создание пустого списка
    }
    // Точка входа
    public static void main (String[] args) {
        Crawler crawler = new Crawler(); // Иницализируем объект класса
        crawler.setSite("http://www.gtp-tabs.ru/", 1);// Здесь прописываем сайт, который мы будем сканировать
        crawler.startParse(); // Начало парса
        crawler.getSites(); // Получение всех сайтов и вывод на экран
    }
    public void startParse() {
        URLDepthPair nowPage = notVisitedList.getFirst(); // Первый элемент списка, последняя страница, которую посещаем
        // Если глубина страницы, меньше глубины, которую мы ищем и при список не посещенных страниц не пустой
        while (nowPage.getDepth() <= depth && !notVisitedList.isEmpty()) {
            // Обновляем текущую страницу.
            nowPage = notVisitedList.getFirst();
            Socket socket = null; // Инициализируем сокет по которому подключаемся пустым значением
            try {
                // Открываем сокет установив имя хоста и порт 80
                socket = new Socket(nowPage.getHostName(), HTTP_PORT);
                System.out.println("Посещаем " + nowPage.getURL() + "."); // Выведем сайт, что посетили
                // Установка таймаута
                try {
                    socket.setSoTimeout(5000); // Ставим таймер на 5 секунд и проверяем таким образом сокет на ошибки
                }
                // Если ошибка была
                catch (SocketException exc) {
                    System.err.println("SocketException: " + exc.getMessage());// Вывести код ошибки
                    socketException(socket); //
                    continue; // Приостановить шаг цикла
                }
                // Для отправки запросов на сервер используем запись данных в сокет
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // Отправка запроса на получение html-страницы
                out.println("GET " + nowPage.getPagePath() + " HTTP/1.1"); // Ставим метод GET и заголовок
                out.println("Host: " + nowPage.getHostName()); // Устанавливаем хоста
                out.println("Connection: close"); // Пишем, что соединение закрыто
                out.println(""); // Перевод строки
                // Получение ответа, сохраняем через буферизированный считыватель
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Проверка на bad request
                String line = in.readLine();
                if (line.startsWith(BAD_REQUEST_LINE)) {
                    System.out.println(line + "\n"); // Выводим проблему запроса в консоли
                    this.socketException(socket); // Прокидываем ошибку
                    continue; // Прерываем шаг цикла
                }
                // Чтение основного файла
                while(line != null) {
                    try {
                        //Извлечнение строки из html-кода
                        line = in.readLine();
                        // Извлечение ссылки из тэга, если она там есть, если нет, идём к следующей строке
                        String url = getURLFromHTMLTag(line);
                        // Если ссылки нет, то заканчиваем шаг цикла
                        if (url == null) continue;
                        //Пропускаем https
                        if (url.startsWith("https://")) {
                            continue;
                        }
                        //Добавляем http
                        if (url.startsWith("http://")){
                            String newURL = cut(url);
                            this.addNewURL(newURL, nowPage.getDepth() + 1);
                        } else if (url.startsWith("/")){
                            //Обрезаем и добавляем всё остальное
                            int index = nowPage.getURL().lastIndexOf("/");
                            String newURL = nowPage.getURL().substring(0, index) + cut(url);
                            this.addNewURL(newURL, nowPage.getDepth() + 1);
                        }
                    }
                    catch (Exception e) {
                        break;
                    }
                }
            }
            // Обработка неопознанного хоста
            catch (UnknownHostException e) {
                System.out.println("UnknownHostException in: " + nowPage.getURL());
            }
            // Обработка ошибок ввода/Вывода
            catch (IOException e) {
                e.printStackTrace();
            }
            //
            endPair(nowPage, socket);
            nowPage = notVisitedList.getFirst();
        }
    }

    // Метод ошибки сокета. Было решено не создавать класс Exception, так как всего один тип ошибки
    private void socketException(Socket socket){
        // Вывод сообщения ошибки
        System.out.println("Ошибка, закрываем сокет. Ошибка в " + this.notVisitedList.getFirst().toString());
        this.notVisitedList.removeFirst(); // Удаляем URL, который добавили
        if (socket == null) return; // Если сокет пустой, закрывать его не надо
        closeSocket(socket); // Если не пустой, закрываем
    }
    private void endPair(URLDepthPair pair, Socket socket){
        visitedList.add(pair); // Добавляем сайт в посещенные
        notVisitedList.removeFirst(); // Удаляем сайт из не посещенных
        closeSocket(socket); // Закрываем сокет-соединение
    }
    // Закрытие сокета
    private void closeSocket(Socket socket){
        // Пытаемся закрыть сокет
        try {
            socket.close();
        }
        // Если ошибка возникла, то распечатаем её
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Парсер URL из HTML тега
    public static String getURLFromHTMLTag(String line) {
        if (line.indexOf(Crawler.HOOK_REF) == -1) return null; // Если тег начинается не на a link, то не парсим
        // Иначе берем все после  <a link до слеша закрытия
        int indexStart = line.indexOf(Crawler.HOOK_REF) + Crawler.HOOK_REF.length();
        int indexEnd = line.indexOf("\"", indexStart);
        // Если слеша закрытия нет, не парсим
        if (indexEnd == -1) return null;
        return line.substring(indexStart, indexEnd); // Берем подстроку без окончания тега и начал
    }
    public static String cut(String url) {
        int index = url.lastIndexOf("#");
        if (index == -1) return url;
        return url.substring(0, index);
    }
    // Добавлеие нового URL
    private void addNewURL(String url, int depth) {
        URLDepthPair newURL = null; // Инициализируем URL
        try{
            newURL = new URLDepthPair(url, depth); // Пытаемся инициализировать новый URL
        } catch (MalformedURLException e) { // Если произошла ошибка, выведем её сообщение
            e.printStackTrace();
        }
        notVisitedList.addLast(newURL); // Если ошибок не было, то добавим URL в список не посещенных
    }
    //Вывод результата
    public void getSites() {
        System.out.println("---------------------------------------\n Посещенные сайты:");
        int count = 1; // Счетчик страниц
        // Обходим все посещенные сайты и печатаем их, увеличивая счетчик на 1
        for (URLDepthPair pair : visitedList) {
            System.out.println(count + " |  " + pair.toString());
            count += 1;
        }

        System.out.println("\nНе посещенные сайты: ");
        count = 1; // Обнуляем счетчик
        // Обходим все не посещенные сайты и печатаем их, увеличивая счетчик на 1
        for (URLDepthPair pair : notVisitedList) {
            System.out.println(count + " |  " + pair.toString());
            count += 1;
        }
    }
    // Задание первого сайта
    public void setSite(String url, int depth){
        // Максимальная глубина
        this.depth = depth;
        // Переменная для корневого сайта
        URLDepthPair firstOne;
        try {
            // Передаем сайт как первый в глубине и ставим её 0
            firstOne = new URLDepthPair(url, 0);
        }catch (MalformedURLException e){ // Обрабатываем ошибку, если URL не смог сформироваться
            e.printStackTrace(); // Выводим ошибку
            return;
        }
        this.notVisitedList.add(firstOne); // Добавляем в посещенные сайты главную страницу
    }
}