package netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Client2 {
    public static void main(String[] args) {
        //线程池，event:io事件，loop：循环，group：池
        EventLoopGroup group = new NioEventLoopGroup(1);//默认核心数*2
        Bootstrap b = new Bootstrap();          //辅助启动类

        try {
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
}