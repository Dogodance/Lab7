import java.lang.Exception;
import java.net.MalformedURLException;
import java.net.*;
import java.io.*;
import java.util.LinkedList;

public class Crawler {
    public static final int HTTP_PORT = 80; // ���� �����������
    public static final String HOOK_REF = "<a href=\""; // ����� ����
    public static final String BAD_REQUEST_LINE = "HTTP/1.1 400 Bad Request"; // Header HTTP �����
    private LinkedList<URLDepthPair> notVisitedList; // ������ �� ���������� �������
    private LinkedList<URLDepthPair> visitedList; // ������ ���������� �������
    int depth;    // ������� ������
    // �����������
    public Crawler() {
        notVisitedList = new LinkedList<URLDepthPair>(); // �������� ������� ������
        visitedList = new LinkedList<URLDepthPair>(); // �������� ������� ������
    }
    // ����� �����
    public static void main (String[] args) {
        Crawler crawler = new Crawler(); // ������������� ������ ������
        crawler.setSite("http://www.gtp-tabs.ru/", 1);// ����� ����������� ����, ������� �� ����� �����������
        crawler.startParse(); // ������ �����
        crawler.getSites(); // ��������� ���� ������ � ����� �� �����
    }
    public void startParse() {
        URLDepthPair nowPage = notVisitedList.getFirst(); // ������ ������� ������, ��������� ��������, ������� ��������
        // ���� ������� ��������, ������ �������, ������� �� ���� � ��� ������ �� ���������� ������� �� ������
        while (nowPage.getDepth() <= depth && !notVisitedList.isEmpty()) {
            // ��������� ������� ��������.
            nowPage = notVisitedList.getFirst();
            Socket socket = null; // �������������� ����� �� �������� ������������ ������ ���������
            try {
                // ��������� ����� ��������� ��� ����� � ���� 80
                socket = new Socket(nowPage.getHostName(), HTTP_PORT);
                System.out.println("�������� " + nowPage.getURL() + "."); // ������� ����, ��� ��������
                // ��������� ��������
                try {
                    socket.setSoTimeout(5000); // ������ ������ �� 5 ������ � ��������� ����� ������� ����� �� ������
                }
                // ���� ������ ����
                catch (SocketException exc) {
                    System.err.println("SocketException: " + exc.getMessage());// ������� ��� ������
                    socketException(socket); //
                    continue; // ������������� ��� �����
                }
                // ��� �������� �������� �� ������ ���������� ������ ������ � �����
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // �������� ������� �� ��������� html-��������
                out.println("GET " + nowPage.getPagePath() + " HTTP/1.1"); // ������ ����� GET � ���������
                out.println("Host: " + nowPage.getHostName()); // ������������� �����
                out.println("Connection: close"); // �����, ��� ���������� �������
                out.println(""); // ������� ������
                // ��������� ������, ��������� ����� ���������������� �����������
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // �������� �� bad request
                String line = in.readLine();
                if (line.startsWith(BAD_REQUEST_LINE)) {
                    System.out.println(line + "\n"); // ������� �������� ������� � �������
                    this.socketException(socket); // ����������� ������
                    continue; // ��������� ��� �����
                }
                // ������ ��������� �����
                while(line != null) {
                    try {
                        //����������� ������ �� html-����
                        line = in.readLine();
                        // ���������� ������ �� ����, ���� ��� ��� ����, ���� ���, ��� � ��������� ������
                        String url = getURLFromHTMLTag(line);
                        // ���� ������ ���, �� ����������� ��� �����
                        if (url == null) continue;
                        //���������� https
                        if (url.startsWith("https://")) {
                            continue;
                        }
                        //��������� http
                        if (url.startsWith("http://")){
                            String newURL = cut(url);
                            this.addNewURL(newURL, nowPage.getDepth() + 1);
                        } else if (url.startsWith("/")){
                            //�������� � ��������� �� ���������
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
            // ��������� ������������� �����
            catch (UnknownHostException e) {
                System.out.println("UnknownHostException in: " + nowPage.getURL());
            }
            // ��������� ������ �����/������
            catch (IOException e) {
                e.printStackTrace();
            }
            //
            endPair(nowPage, socket);
            nowPage = notVisitedList.getFirst();
        }
    }

    // ����� ������ ������. ���� ������ �� ��������� ����� Exception, ��� ��� ����� ���� ��� ������
    private void socketException(Socket socket){
        // ����� ��������� ������
        System.out.println("������, ��������� �����. ������ � " + this.notVisitedList.getFirst().toString());
        this.notVisitedList.removeFirst(); // ������� URL, ������� ��������
        if (socket == null) return; // ���� ����� ������, ��������� ��� �� ����
        closeSocket(socket); // ���� �� ������, ���������
    }
    private void endPair(URLDepthPair pair, Socket socket){
        visitedList.add(pair); // ��������� ���� � ����������
        notVisitedList.removeFirst(); // ������� ���� �� �� ����������
        closeSocket(socket); // ��������� �����-����������
    }
    // �������� ������
    private void closeSocket(Socket socket){
        // �������� ������� �����
        try {
            socket.close();
        }
        // ���� ������ ��������, �� ����������� �
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ������ URL �� HTML ����
    public static String getURLFromHTMLTag(String line) {
        if (line.indexOf(Crawler.HOOK_REF) == -1) return null; // ���� ��� ���������� �� �� a link, �� �� ������
        // ����� ����� ��� �����  <a link �� ����� ��������
        int indexStart = line.indexOf(Crawler.HOOK_REF) + Crawler.HOOK_REF.length();
        int indexEnd = line.indexOf("\"", indexStart);
        // ���� ����� �������� ���, �� ������
        if (indexEnd == -1) return null;
        return line.substring(indexStart, indexEnd); // ����� ��������� ��� ��������� ���� � �����
    }
    public static String cut(String url) {
        int index = url.lastIndexOf("#");
        if (index == -1) return url;
        return url.substring(0, index);
    }
    // ��������� ������ URL
    private void addNewURL(String url, int depth) {
        URLDepthPair newURL = null; // �������������� URL
        try{
            newURL = new URLDepthPair(url, depth); // �������� ���������������� ����� URL
        } catch (MalformedURLException e) { // ���� ��������� ������, ������� � ���������
            e.printStackTrace();
        }
        notVisitedList.addLast(newURL); // ���� ������ �� ����, �� ������� URL � ������ �� ����������
    }
    //����� ����������
    public void getSites() {
        System.out.println("---------------------------------------\n ���������� �����:");
        int count = 1; // ������� �������
        // ������� ��� ���������� ����� � �������� ��, ���������� ������� �� 1
        for (URLDepthPair pair : visitedList) {
            System.out.println(count + " |  " + pair.toString());
            count += 1;
        }

        System.out.println("\n�� ���������� �����: ");
        count = 1; // �������� �������
        // ������� ��� �� ���������� ����� � �������� ��, ���������� ������� �� 1
        for (URLDepthPair pair : notVisitedList) {
            System.out.println(count + " |  " + pair.toString());
            count += 1;
        }
    }
    // ������� ������� �����
    public void setSite(String url, int depth){
        // ������������ �������
        this.depth = depth;
        // ���������� ��� ��������� �����
        URLDepthPair firstOne;
        try {
            // �������� ���� ��� ������ � ������� � ������ � 0
            firstOne = new URLDepthPair(url, 0);
        }catch (MalformedURLException e){ // ������������ ������, ���� URL �� ���� ��������������
            e.printStackTrace(); // ������� ������
            return;
        }
        this.notVisitedList.add(firstOne); // ��������� � ���������� ����� ������� ��������
    }
}