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

import bio.overture.archetype.grpc_template.grpc.interceptor.AuthInterceptor;
import bio.overture.archetype.grpc_template.properties.AppProperties;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

import static bio.overture.archetype.grpc_template.properties.AppProperties.APP_PROPERTIES_PREFIX;

@Slf4j
@Component
@ConditionalOnProperty(value= APP_PROPERTIES_PREFIX+"."+AppProperties.Fields.grpcEnabled)
//@ConditionalOnProperty(value= "app.grpc-enabled")
public class GRpcServerRunner implements CommandLineRunner, DisposableBean {

  private Server server;

  private final CarServiceImpl carServiceImpl;
  private final AuthInterceptor authInterceptor;
  private final HealthStatusManager healthStatusManager;
  private final AppProperties appProperties;


  @Autowired
  public GRpcServerRunner(@NonNull CarServiceImpl carServiceImpl,
      @NonNull AuthInterceptor authInterceptor,
      @NonNull AppProperties appProperties ) {
    this.carServiceImpl = carServiceImpl;
    this.authInterceptor = authInterceptor;
    this.healthStatusManager = new HealthStatusManager();
    this.appProperties = appProperties;
  }

  @Override
  public void run(String... args) throws Exception {

    // Interceptor bean depends on run profile.
    val templateCarService = ServerInterceptors.intercept(carServiceImpl, authInterceptor);
    healthStatusManager.setStatus("car_service.CarService", ServingStatus.SERVING);

    try{
      server = ServerBuilder.forPort(appProperties.getGrpcPort())
          .addService(templateCarService)
          .addService(ProtoReflectionService.newInstance())
          .addService(healthStatusManager.getHealthService())
          .build()
          .start();
    } catch (IOException e) {
      log.error("gRPC server cannot be started: {}", e.getMessage());
    }

    log.info("gRPC Server started, listening on port " + appProperties.getGrpcPort());
    startDaemonAwaitThread();
  }

  private void startDaemonAwaitThread() {
    Thread awaitThread = new Thread(()->{
      try {
        this.server.awaitTermination();
      } catch (InterruptedException e) {
        log.error("gRPC server stopped: {}", e.getMessage());
      }
    });
    awaitThread.start();
  }

  @Override
  public final void destroy() throws Exception {
    log.info("Shutting down gRPC server ...");
    Optional.ofNullable(server).ifPresent(Server::shutdown);
    log.info("gRPC server stopped.");
  }
}
