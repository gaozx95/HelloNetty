package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

public class Server {
    //静态通道组 ，将所有连接（channelActive，通道可以用的时候）的channel加入到通道组
    public static ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);    //对通道组（clients）上的事件进行处理的线程（池）

    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(2);

        ServerBootstrap b = new ServerBootstrap();
        try {
            ChannelFuture f =
                    b.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pl = socketChannel.pipeline();
                            pl.addLast(new ServerChildHandler());
                        }
                    })
                    .bind(8888)
                    .sync();
            System.out.println("server started!");
            f.channel().closeFuture().sync();       //调用close()方法会触发closeFuture


        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}

class ServerChildHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Server.clients.add(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = null;
        try {
            buf = (ByteBuf) msg;

            byte[] bytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(),bytes);
            System.out.println(new String(bytes));

            Server.clients.writeAndFlush(msg);          //将通道组里所有的通道依次拿出来写数据

        }finally {
//            if(buf != null){
//                ReferenceCountUtil.release(buf);        //释放内存
//            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();        //执行完close后client端会触发closeFuture，从而结束client端
    }
}