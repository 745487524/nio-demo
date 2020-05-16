package com.imooc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.rmi.server.ExportException;
import java.util.Iterator;
import java.util.Set;

public class NIOServer {

    public void start() throws IOException {
        /**
         * 1、创建Channel，绑定端口
         * 2、设置Channel为非阻塞
         * 3、创建ServerSelector
         * 4、将Channel绑定到Selector
         * 5、Channel循环监听连接
         * 6、根据相应的就绪状态执行相应的方法
         */
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.bind(new InetSocketAddress(8100));
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("NIOServer服务启动成功……");
        for (; ; ) {
            int receiveNum = selector.select();

            if (receiveNum == 0) continue;

            Set<SelectionKey> selectionKeys = selector.selectedKeys();

            Iterator iterator = selectionKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey selectionKey = (SelectionKey) iterator.next();

                iterator.remove();
                //判断接入事件
                if (selectionKey.isValid() && selectionKey.isAcceptable()) {
                    //如果是接入事件
                    acceptHandler(channel, selector);
                }
                if (selectionKey.isValid() && selectionKey.isReadable()) {
                    //如果是读取事件
                    readHandler(selectionKey, selector);
                }

            }

        }
    }

    private static void readHandler(SelectionKey selectionKey, Selector selector) {
        /**
         * 如果是读取事件
         * 1、从selectKey中获取就绪的Channel
         * 2、将Channel设置为非阻塞
         * 3、从Channel中读取数据并进行整合
         * 4、发送消息到其他客户端
         */
        try {
            SocketChannel channel = (SocketChannel) selectionKey.channel();
            channel.configureBlocking(false);
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            StringBuffer sb = new StringBuffer();
            while (channel.read(byteBuffer) > 0) {

                /**
                 * bytebuffer存储从channel中读取出来的数据
                 * 将bytebuffer切换为写模式，即将数组索引位置重置为0，下次开始重新写入从Channel中新读取的数据，
                 * 可读数组的最大长度为limit即从channel中读取的字节长度
                 * position置为0，使下次读取数据写入到数组中时从数组的开始位置进行写入
                 * 将mark标记清空避免其他操作充值position位置
                 * public final Buffer flip() {
                 *   limit = position; //将数组的上限设为当前position所在的位置，即可以从数组中读取的数据的大小
                 *   position = 0; //将position设置为0开始，即将索引位置设置为数组的开始位置0
                 *   mark = -1; //将mark标记置为-1，mark存储标记的position的位置
                 *   return this; //返回这个bytebuffer对象
                 * }
                 */
                byteBuffer.flip();
                sb.append(Charset.forName("UTF-8").decode(byteBuffer));
            }
            //将Channel再次注册到Selector上并监听可读事件（准确来说是唤醒读写的Channel，并设置Channel状态为可读模式）
            channel.register(selector, SelectionKey.OP_READ);
            //将消息发送到其他客户端
            if (sb.length() > 0) {
                System.out.println(channel.getRemoteAddress() + "：" + sb.toString());
                broadCast(selector, channel, sb.toString());
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    if (!key.channel().equals(channel)) {
                        //向channel中发送回复元素
                        channel.write(Charset.forName("UTF-8").encode(sb.toString()));
                    }
                }
            }
        } catch (Exception e) {
            selectionKey.cancel();
        }

    }

    /**
     * 将客户端发来的信息进行广播给其他客户端
     * @param selector
     * @param sourceChannel
     * @param request
     */
    private static void broadCast(Selector selector, SocketChannel sourceChannel, String request) {
        Set<SelectionKey> selectionKeys = selector.keys();
        /**
         * 1、AClient进行读写的Channel
         * 2、BClient进行读写的Channel   ->  客户端进行读写的Channel数量
         * 3、selector绑定端口的Channel  ->  绑定了几个端口轮询的Channel信道就存在多少轮询Channel
         */
        selectionKeys.forEach(selectionKey -> {
            Channel channel = selectionKey.channel();
            if (channel instanceof SocketChannel && !channel.equals(sourceChannel)) {
                try {
                    ((SocketChannel) channel).write(Charset.forName("UTF-8").encode(request));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void acceptHandler(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        /**
         * 如果是接入事件
         * 1、新建一个Channel，设置为非阻塞模式
         * 2、将Channel绑定到selector，注册可读事件
         * 3、向客户端返回信息
         */
        SocketChannel channel = serverSocketChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        channel.write(Charset.forName("UTF-8").encode("你与聊天室中其他人不是好友关系，请注意隐私！"));
    }

    public static void main(String[] args) throws IOException {
        NIOServer server = new NIOServer();
        server.start();
    }
}
