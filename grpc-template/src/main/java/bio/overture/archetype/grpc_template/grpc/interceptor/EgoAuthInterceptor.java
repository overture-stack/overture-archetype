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

package bio.overture.archetype.grpc_template.grpc.interceptor;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import bio.overture.archetype.grpc_template.services.EgoService;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("auth")
public class EgoAuthInterceptor implements AuthInterceptor {

  private final EgoService egoService;

  public static final Context.Key<EgoService.EgoToken> EGO_TOKEN = Context.key("egoToken");

  public static final Metadata.Key<String> JWT_METADATA_KEY =
      Metadata.Key.of("jwt", ASCII_STRING_MARSHALLER);

  @Autowired
  public EgoAuthInterceptor(EgoService egoService) {
    this.egoService = egoService;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
    // You need to implement validateIdentity
    String token = metadata.get(JWT_METADATA_KEY);
    val egoToken = egoService.verifyToken(token);
    Context context = Context.current().withValue(EGO_TOKEN, egoToken.orElse(null));
    return Contexts.interceptCall(context, call, metadata, next);
  }

  /** Handling authorization */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface EgoAuth {
    String[] typesAllowed() default {"ADMIN", "USER"};

    @Aspect
    @Component
    @Slf4j
    @Profile("auth")
    class EgoAuthAspect {

      @SneakyThrows
      @Around("@annotation(egoAuth)")
      public Object checkIdentIty(ProceedingJoinPoint pjp, EgoAuth egoAuth) {
        val egoToken = EGO_TOKEN.get();
        val call = Iterables.get(List.of(pjp.getArgs()), 1, null);
        assert call instanceof StreamObserver;

        if (egoToken == null) {
          ((StreamObserver) call)
              .onError(new StatusException(Status.fromCode(Status.Code.UNAUTHENTICATED)));
          return null;
        }

        val availableRoles =
            Sets.intersection(Set.of(egoAuth.typesAllowed()), Set.of(egoToken.getType()));

        if (availableRoles.isEmpty()) {
          ((StreamObserver) call)
              .onError(new StatusException(Status.fromCode(Status.Code.PERMISSION_DENIED)));
          return null;
        } else {
          try {
            return pjp.proceed();
          } catch (Throwable e) {
            log.info(e.getMessage());
            throw e;
          }
        }
      }
    }
  }
}