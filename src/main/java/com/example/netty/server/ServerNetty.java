package com.example.netty.server;

import com.example.netty.handler.ServerChannelInitializer;
import com.example.netty.handler.ServerUAVHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 *  tcp/ip 服务端用netty实现
 */
@Slf4j
@Component
@AllArgsConstructor
@NoArgsConstructor
public class ServerNetty {

    private int port = 9000;

    public ServerNetty(int port){
        this.port = port;
    }

    //一个主线程组
    EventLoopGroup bossGroup  = new NioEventLoopGroup(1);

    //2 第二个线程组 是用于实际的业务处理操作的
    // 用来处理已经被接收的连接，一旦bossGroup接收到连接，就会把连接信息注册到workerGroup上
    EventLoopGroup workGroup = new NioEventLoopGroup(20);
    //服务端启动
    @PostConstruct
    public void start(){

        // nio服务的启动类
        ServerBootstrap sbs = new ServerBootstrap();
        // 配置nio服务参数
        sbs.group(bossGroup,workGroup) //是将两个线程组传入，让其工作。
            .channel(NioServerSocketChannel.class)  //我要指定使用NioServerSocketChannel这种类型的通道
            .option(ChannelOption.SO_BACKLOG,1024) //  tcp最大缓存链接个数 配置TCP参数，将其中一个参数backlog设置为1024，表明临时存放已完成三次握手的请求的队列的最大长度。
            .childOption(ChannelOption.SO_KEEPALIVE,true) //设置TCP长连接,一般如果两个小时内没有数据的通信时,TCP会自动发送一个活动探测数据报文。
            .handler(new LoggingHandler(LogLevel.INFO))  //打印日志级别
                //一定要使用 childHandler 去绑定具体的 事件处理器
                //用于处理客户端的IO事件，比如有一个客户端发起请求，要读取数据，就可以使用这里面的类来处理这个事件。
                // 这是整个处理的核心。也是我们自己主要关注的类。
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    // 这里相当于过滤器，可以配置多个 ServerUAVHandler使我们主要去操作的类
                    socketChannel.pipeline().addLast(new ServerChannelInitializer());
                }
            });
        try {
            // 绑定端口，开始接受链接
            //使用sync方法阻塞一直到绑定成功。
            ChannelFuture cf = sbs.bind(port).sync();
            // 开多个端口
//          ChannelFuture cf2 = sbs.bind(3333).sync();
//          cf2.channel().closeFuture().sync();
            if (cf.isSuccess()){
                log.info("server开启");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    //优雅退出
    @PreDestroy
    private void destroy(){
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
        log.info("server关闭");
    }

    /**
     * 其实上面的这些代码都是固定模式的，也就是说你使用的时候直接就可以用了，
     * 最核心的就是如何处理客户端的各种事件，
     * 比如说有客户端连接、读、写数据等等相关事件。看看ServerUAVHandler类是如何实现的，
     */

    /**
     *
     * 解决数据传输中中的拆包和粘包问题方案：
     *
     * 一 . 用特定字符当做分隔符，例如：$_
     *  （1） 将下列代码添加到 initChannel方法内
     //将双方约定好的分隔符转成buf
     ByteBuf bb = Unpooled.copiedBuffer("$_".getBytes(Constant.charset));
     socketChannel.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, bb));
     //将接收到信息进行解码，可以直接把msg转成字符串
     socketChannel.pipeline().addLast(new StringDecoder());

     （2） 在 ServerHandler中的 channelRead方法中应该替换内容为
     // 如果把msg直接转成字符串，必须在服务中心添加 socketChannel.pipeline().addLast(new StringDecoder());
     String reqStr = (String)msg;
     System.err.println("server 接收到请求信息是："+reqStr);
     String respStr = new StringBuilder("来自服务器的响应").append(reqStr).append("$_").toString();
     // 返回给客户端响应
     ctx.writeAndFlush(Unpooled.copiedBuffer(respStr.getBytes()));

     (3) 因为分隔符是双方约定好的，在ClientNetty和channelRead中也应该有响应的操作


     二. 双方约定好是定长报文
     // 双方约定好定长报文为6，长度不足时服务端会一直等待直到6个字符，所以客户端不足6个字符时用空格补充；其余操作，参考分隔符的情况
     socketChannel.pipeline().addLast(new FixedLengthFrameDecoder(6));


     三. 请求分为请求头和请求体，请求头放的是请求体的长度；一般生产上常用的

     （1）通信双方约定好报文头的长度，先截取改长度，
     （2）根据报文头的长度读取报文体的内容
     *
     *
     */

}
