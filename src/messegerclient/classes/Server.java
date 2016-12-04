/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package messegerclient.classes;

/**
 *
 * @author Vovan
 */

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;




public class Server {
    String ips = "127.0.0.1";
    JTextField text;
    JTextArea textArea;
    InputStream read;
    OutputStream writer;
    FileOutputStream fileWriter;
    String userName;
    String pass;
    Thread readThread = null;
    Socket socket;
    boolean isFile = false;
    boolean isISendFile = false;
    /**
     *  the command line arguments
     */
    
    public Server(JTextArea tArea,JTextField tField,String l, String p,String ip){
        userName = l;
        pass = p;
        textArea = tArea;
        text = tField;
        ips = ip;
        
    }
    
    public void stop(){
        text = null;
        textArea = null;
        read = null ;
        writer = null;
        userName = null;
        pass = null;
        readThread = null;
        socket = null; 
    }
    
    
    public boolean start(){
        boolean isUser = checkUserInBase();
        if(isUser){
            setUpNetWorking();
            readThread = new Thread(new IncomingReader());
            readThread.start();
            return true;
        }
        return false;
    }
    
    public boolean registration(){
        Socket socket;
        try {
            socket = new Socket(ips, 6869);
            InputStreamReader streamReader = new InputStreamReader(socket.getInputStream());
            BufferedReader read = new BufferedReader(streamReader);
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            System.out.print("networking esteblished");
            Thread.sleep(4000);
            //відправляю команду
            writer.println("2");
            writer.flush();
            Thread.sleep(2000);
            //відправляю дані користувача
            writer.println(userName + "|" + pass);
            writer.flush();
            //ловлю відповідь чи норм чи ні
            String isUser;
            while((isUser = read.readLine())!= null){
                if(isUser.equals("true")){
                    return true;
                }else{
                    return false;
                }
            }
        } catch (IOException ex) {} catch (InterruptedException ex) { return false; } 
        return false;
    }
    
    //встановлює звязок з сервером для чата
    private void setUpNetWorking(){
        try {
            socket = new Socket(ips, 6868);
            read = socket.getInputStream();
            writer = socket.getOutputStream();
            System.out.print("networking esteblished");
        } catch(IOException ex){ex.printStackTrace();}
        
    }
    
    //надсилання повідомлення
    public void sendText() {   
            try {
                writer.write((userName + ": " + text.getText()).getBytes());
                writer.flush();
            }
            catch(Exception ea){ea.printStackTrace();}
            text.setText("");
            text.requestFocus();       
    }

    public void sendFile(File file) {
        textArea.append("Відправлення файла: "+file.getName()+"\n");
            isISendFile = true;
            new Thread(() -> {
                try {

                    FileInputStream in = new FileInputStream(file);
                    writer.write(("~#file~#*|"+file.getName()).getBytes());
                    writer.flush();

                    int count;
                    long length = file.length();
                    byte[] bytes = new byte[16 * 1024];
                    while ((count = in.read(bytes)) > 0) {
                        writer.write(bytes, 0, count);
                    }
                    writer.flush();

                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            writer.write(("~#end~#*").getBytes());
                            writer.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                } catch(Exception ea){
                    ea.printStackTrace();
                }
            }).start();

        text.setText("");
        text.requestFocus();
    }

    //читання повідомлення
    private class IncomingReader implements Runnable{
        public void run(){
            String message = null;
            try {

                int count;
                byte[] buffer = new byte[8192]; // or 4096, or more
                    while ((count = read.read(buffer)) > 0){
                        System.out.println("read " + new String(buffer));
                        message = new String(buffer, 0, count);
                        if (!isFile && message.contains("~#file~#*")){
                                String name = message.split("\\|")[1];
                                isFile = true;
                                if (!isISendFile){
                                    textArea.append("Приймаэться файл " + name+"\n");
                                    File file = new File(name);
                                    fileWriter = new FileOutputStream(file);
                                }
                                continue;
                        }

                        if (isFile && message.contains("~#end~#*")){
                            if (!isISendFile){
                                textArea.append("Файл успішно завантажений"+"\n");
                                fileWriter.flush();
                                fileWriter.close();
                            } else {
                                textArea.append("Файл успышно відправлений\n");
                            }
                            isFile = false;
                            isISendFile = false;
                            continue;
                        }

                        if (isFile){
                            if (!isISendFile){
                                fileWriter.write(buffer, 0 , count);
                            }
                        } else {
                            textArea.append(message + "\n");
                        }
                    }
            }
            catch(Exception ex){ex.printStackTrace();}
        }
    }
    
    //встановлює звязок з сервером для провіки юзера
    public boolean checkUserInBase(){
        try {
            Socket socket = new Socket(ips, 6869);
            InputStreamReader streamReader = new InputStreamReader(socket.getInputStream());
            BufferedReader read = new BufferedReader(streamReader);
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            System.out.print("networking esteblished");
            Thread.sleep(5000);
            //відправляю на перевірку користувача в базі
            writer.println("1");
            writer.flush();
            Thread.sleep(2000);
            writer.println(userName + "|" + pass);
            writer.flush();
            //ловлю відповідь
            String isUser;
            while((isUser = read.readLine())!= null){
                if(isUser.equals("true")){
                    return true;
                }else{
                    return false;
                }
            }
        } catch(IOException ex){
            ex.printStackTrace();
            return false;
        } catch (InterruptedException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
}
