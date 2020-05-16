package com.imooc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class AClient {

    public void start(String nickName) throws IOException {
        /**
         * 1、连接服务端
         * 2、向服务器端发送数据
         * 3、接收服务器端的反馈
         */
        SocketChannel channel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8100));
        //接收服务器端消息
        Selector selector = Selector.open();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        new Thread(new NIOClientTread(selector)).start();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String request = scanner.nextLine();
            if (request != null && request.length() > 0) {
                channel.write(Charset.forName("UTF-8").encode(nickName + "：" + request));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new AClient().start("AClient");
    }
}
