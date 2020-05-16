package com.imooc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class NIOClientTread implements Runnable {

    private Selector selector;

    public NIOClientTread(Selector selector){
        this.selector = selector;
    }

    @Override
    public void run() {
        //获取Channel
        //将Channel注册到Selector
        //接收Channel中的数据
        try {
            for (; ; ) {

                int receiveNum = selector.select();

                if (receiveNum == 0) continue;

                Set<SelectionKey> selectionKeys = selector.selectedKeys();

                Iterator iterator = selectionKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey selectionKey = (SelectionKey) iterator.next();
                    if (selectionKey.isReadable()){
                        readHandler(selectionKey, selector);
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException {
        /**
         * 如果是读取事件
         * 1、从selectKey中获取就绪的Channel
         * 2、将Channel设置为非阻塞
         * 3、从Channel中读取数据并进行整合
         * 4、发送消息到其他客户端
         */
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        channel.configureBlocking(false);
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        StringBuffer sb = new StringBuffer();
        while (channel.read(byteBuffer) > 0) {
            //将bytebuffer切换为写模式
            byteBuffer.flip();
            sb.append(Charset.forName("UTF-8").decode(byteBuffer));
        }
        //将Channel再次注册到Selector上并监听可读事件
        channel.register(selector, SelectionKey.OP_READ);
        //接收服务端消息
        if (sb.length() > 0) {
            System.out.println(sb.toString());
        }
    }
}
