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

import bio.overture.archetype.grpc_template.client.EgoClient;
import bio.overture.archetype.grpc_template.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import bio.overture.archetype.grpc_template.model.CarModel;
import bio.overture.archetype.grpc_template.model.DriveType;
import bio.overture.proto.car_service.CarServiceGrpc;
import bio.overture.proto.car_service.CreateCarRequest;
import bio.overture.proto.car_service.CreateCarResponse;
import bio.overture.proto.car_service.GetCarRequest;
import bio.overture.proto.car_service.GetCarResponse;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Slf4j
@Service
public class CarServiceImpl extends CarServiceGrpc.CarServiceImplBase implements Healthable {

  private final EgoClient egoClient;

  @Autowired
  public CarServiceImpl(@NonNull EgoClient egoClient) {
    this.egoClient = egoClient;
  }

  @Override
  public String getHealthCheckName() {
    return CarServiceGrpc.SERVICE_NAME;
  }

  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void createCar(
      CreateCarRequest request, StreamObserver<CreateCarResponse> responseObserver) {
    log.info("Storing car: {}", request.toString());

    val newId = UUID.randomUUID();
    val carData = request.getCar();

    val carModel =
        CarModel.builder()
            .id(newId)
            .brand(carData.getBrand())
            .year(carData.getDateCreated().getYear())
            .electric(carData.getElectric())
            .horsepower(carData.getHorsepower())
            .model(carData.getModel())
            .type(DriveType.resolveDriveType(carData.getType().name()).orElse(null))
            .build();

    // do something with carModel...

    // For example, list the first 100 users from ego
    val users = egoClient.listUsers(0, 100, "");

    val response = CreateCarResponse.newBuilder().setId(newId.toString()).setCar(carData).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  @EgoAuth(typesAllowed = {"USER"})
  public void getCar(GetCarRequest request, StreamObserver<GetCarResponse> responseObserver) {
    log.info("Reading the car for id: {}", request.getId());
    val response = GetCarResponse.newBuilder().setId(request.getId()).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
