package bio.overture.archetype.grpc_template.util;

import lombok.SneakyThrows;
import lombok.val;
import org.springframework.boot.Banner;
import org.springframework.boot.ResourceBanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;

import java.io.PrintStream;

import static com.github.lalyos.jfiglet.FigletFont.convertOneLine;
import static java.util.Arrays.stream;

public class ProjectBanner implements Banner {
  /**
   * Other fonts can be found at http://www.figlet.org/examples.html
   * */
  private static final String BANNER_FONT_LOC = "/banner-fonts/slant.flf";

  @Override
  @SneakyThrows
  public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
    val text = generateBannerText(environment);
    val resource = new ByteArrayResource(text.getBytes());
    val resourceBanner = new ResourceBanner(resource);
    resourceBanner.printBanner(environment, sourceClass, out);
  }

  @SneakyThrows
  private static String generateBannerText(Environment env){
    val applicationName = env.getProperty("server.banner.text");
    val text = convertOneLine("classpath:"+BANNER_FONT_LOC, applicationName);
    val sb = new StringBuilder();
    stream(text.split("\n"))
        .forEach(t -> sb.append("${Ansi.GREEN} ")
            .append(t)
            .append("\n"));
    sb.append("${Ansi.RED}  :: Spring Boot${spring-boot.formatted-version} :: ${Ansi.DEFAULT}\n");
    return sb.toString();
  }

}
