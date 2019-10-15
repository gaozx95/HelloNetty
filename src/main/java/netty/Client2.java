package netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;

public class Client2 {
    public static void main(String[] args) {
        //线程池，event:io事件，loop：循环，group：池
        EventLoopGroup group = new NioEventLoopGroup(1);//默认核心数*2
        Bootstrap b = new Bootstrap();          //辅助启动类

        try {
            ChannelFuture f =
                b.group(group)              //把线程池放进来，以后事件用这个线程池处理
                    .channel(NioSocketChannel.class)        //指定通道类型
                    .handler(new ClientChannelInitializer())      //当channel上有事件发生，交给handler处理
                    .connect("localhost",8888)  //连接远程服务器，connect是异步方法，调用完就继续往下执行
                    .sync();                                      //connect是异步方法，用sycn方法确认connect执行完再往下执行
            /*//不用sync的写法
            ChannelFuture f = b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ClientChannelInitializer())
                    .connect("localhost",8888);

            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if(!channelFuture.isSuccess()){
                        System.out.println("not connected");
                    }else {
                        System.out.println("connected!");
                    }
                }
            });
            f.sync();
            */
            System.out.println("...");
            f.channel().closeFuture().sync();       //注意阻塞住client，否则执行完就会接受程序，关闭channel，无法收到server的返回值
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            group.shutdownGracefully();
        }
    }

}

class ClientChannelInitializer extends ChannelInitializer<SocketChannel>{

    @Override
    //channel初始化时候调用initChannel方法
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(new ClientHandler2());
    }
}

class ClientHandler2 extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //channel第一次连上可用，写出一个字符串    Direct Memory直接访问内存，跳过垃圾回收机制
        ByteBuf buf = Unpooled.copiedBuffer("hello".getBytes());
        ctx.writeAndFlush(buf);         //会自动释放buf连接

    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = null;
        try {
            buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];       //获取可读的字节数
            buf.getBytes(buf.readerIndex(),bytes);              //将从读指针开始的字节读取到bytes中
            System.out.println(new String(bytes));
        }finally {
              if(buf != null){
                    ReferenceCountUtil.release(buf);        //释放内存
                }
//            System.out.println(buf.refCnt());
        }
    }
}