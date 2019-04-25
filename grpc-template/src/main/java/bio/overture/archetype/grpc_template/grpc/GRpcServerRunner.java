/*
 * Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package bio.overture.archetype.grpc_template.grpc;

import static java.util.Objects.isNull;

import bio.overture.archetype.grpc_template.grpc.interceptor.AuthInterceptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GRpcServerRunner implements CommandLineRunner, DisposableBean {

  private Server server;

  private final AuthInterceptor authInterceptor;
  private final CarServiceImpl carServiceImpl;

  @Autowired
  public GRpcServerRunner(CarServiceImpl carServiceImpl, AuthInterceptor authInterceptor) {
    this.carServiceImpl = carServiceImpl;
    this.authInterceptor = authInterceptor;
  }

  @Override
  public void run(String... args) throws Exception {
    int port = 50051;

    // Interceptor bean depends on run profile.
    val templateCarService = ServerInterceptors.intercept(carServiceImpl, authInterceptor);

    server =
        ServerBuilder.forPort(port)
            .addService(templateCarService)
            .addService(ProtoReflectionService.newInstance())
            .build()
            .start();

    log.info("gRPC Server started, listening on port " + port);
    startDaemonAwaitThread();
  }

  private void startDaemonAwaitThread() {
    Thread awaitThread =
        new Thread(
            () -> {
              try {
                this.server.awaitTermination();
              } catch (InterruptedException e) {
                log.error("gRPC server stopped.", e);
              }
            });
    awaitThread.start();
  }

  @Override
  public final void destroy() throws Exception {
    log.info("Shutting down gRPC server ...");
    if (!isNull(server)) {
      server.shutdown();
    }
    log.info("gRPC server stopped.");
  }
}
