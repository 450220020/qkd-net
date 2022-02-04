package com.uwaterloo.iqc.qnl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import com.uwaterloo.iqc.qnl.lsrp.LSRPRouter;
import com.uwaterloo.iqc.qnl.qll.cqptoolkit.client.GrpcClient;
import com.uwaterloo.iqc.qnl.qll.cqptoolkit.server.ISiteAgentServer;
import com.uwaterloo.iqc.qnl.qll.cqptoolkit.server.KeyTransferServer;

import java.io.IOException;

public class KeyRouter {

    private static Logger LOGGER = LoggerFactory.getLogger(KeyRouter.class);

    public static void main(String[] args) throws Exception {
        QNLConfiguration qConfig;
        if (args.length == 0)
          qConfig = new QNLConfiguration(null);
        else
          qConfig = new QNLConfiguration(args[0]);

        final KeyTransferServer server = new KeyTransferServer(qConfig);
        server.start();

        // String url = null;
        // if(args.length == 2 && "-c".equals(args[0])) {
        //   JSONObject json = (JSONObject) new JSONParser().parse(new FileReader(args[1]));
        //   url = (String) json.get("connectionAddress") + ":" + (String) json.get("listenPort");
        // } else {
        //   LOGGER.error("Unable to start site agent: no config file specified");
        // }
        // final ISiteAgentServer siteAgent = new ISiteAgentServer(url);
        // if(url != null) {
        //   siteAgent.start();
        // }
        // TODO: remove/move magic values such as "-c" and "connectionAddress"
        // TODO: standardize clean way of processing input flags
        // TODO: implement more flexible logic like above instead of hardcoding

        LOGGER.info("starting site agent a");
        final ISiteAgentServer siteAgent = new ISiteAgentServer("192.168.1.237", 8080);
        siteAgent.start();
        LOGGER.info("finished starting site agent a");

        GrpcClient client = new GrpcClient();
        //client.getSiteDetails("localhost", 8000);
        //client.startNode("localhost", 8000, "localhost", 8001);
        LOGGER.info("Key router started, args.length:" + args.length);

        // LSRPRouter lsrpRouter = new LSRPRouter(qConfig);
        // lsrpRouter.start();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new KeyServerRouterInitializer(qConfig))
            .childOption(ChannelOption.AUTO_READ, false)
            .bind(qConfig.getConfig().getPort()).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
