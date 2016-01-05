package vsn;

import java.lang.RuntimeException;
import java.lang.String;
import java.lang.UnsupportedOperationException;
import java.util.Objects;
import javax.comm.CommPortIdentifier;
import org.apache.commons.cli.*;
import vsn.com.*;
import vsn.comm.SerialPort;
import vsn.comm.SerialPortMock;

public class Cli {

  private CommandLine cli = null;
  private CommandLineParser parser;
  private CommPortIdentifier serialPortId;
  private HelpFormatter helpFormat;
  private Options options = new Options();
  private SerialPort serialPort;

  public Cli() {
    // Declara el CLI
    Option audio =  Option.builder("a")
                          .longOpt("audio")
                          .hasArg()
                          .argName("canal")
                          .desc("canal audio de entrada")
                          .build();
    Option help  =  Option.builder("h")
                          .longOpt("help")
                          .desc("muestra este mensaje de ayuda y termina")
                          .build();
    Option input =  Option.builder("i")
                          .longOpt("input")
                          .hasArg()
                          .argName("canal")
                          .desc("canal de entrada, sobrescribe el canal de entrada por defecto, p. e.:\nvsn -type " +
                                "video -i 10 -v 5   // canal de entrada 10")
                          .build();
    Option output = Option.builder("o")
                          .longOpt("output")
                          .hasArg()
                          .argName("canal")
                          .desc("canal de salida")
                          .build();
    Option type =   Option.builder("t")
                          .longOpt("type")
                          .hasArg()
                          .argName("comando")
                          .desc("tipo de comando, los comandos soportados son both (conmutar ambos), audio (conmutar " +
                                "audio), video (conmutar vídeo)")
                          .required()
                          .build();
    Option video =  Option.builder("v")
                          .longOpt("video")
                          .hasArg()
                          .argName("canal")
                          .desc("canal vídeo de entrada")
                          .build();
    options.addOption(audio)
           .addOption(help)
           .addOption(input)
           .addOption(output)
           .addOption(type)
           .addOption(video);
    parser = new DefaultParser();
    helpFormat = new HelpFormatter();
    serialPort = new SerialPortMock();
  }

  public void usage() {
    helpFormat.printHelp("vsn", options);
  }

  public void parse(String[] args) {
    // Procesa las opciones
    try { cli = parser.parse(options, args); }
    catch (MissingOptionException e) { usage(); return; }
    catch (ParseException e) { throw new RuntimeException(e); }
    if (cli.hasOption("h")) { usage(); return;}
    // Inicia la conexión serie
    serialPort.init(serialPortId);
    // Configura el comando
    String commandType = cli.getOptionValue("t");
    byte[] commandBytes = null;
    String inputChStr;
    switch (commandType) {
      case "audio":
        inputChStr = Objects.nonNull(cli.getOptionValue("i")) ?
                       cli.getOptionValue("i") : cli.getOptionValue("a");
        commandBytes = new SwitchAudio(inputChStr, cli.getOptionValue("o")).getBytes();
        break;
      case "both":
        inputChStr = Objects.nonNull(cli.getOptionValue("i")) ?
                       cli.getOptionValue("i") : cli.getOptionValue("v");
        commandBytes = new SwitchBoth(cli.getOptionValue("a"),
                                      cli.getOptionValue("o"),
                                      inputChStr).getBytes();
        break;
      case "video":
        inputChStr = Objects.nonNull(cli.getOptionValue("i")) ?
                       cli.getOptionValue("i") : cli.getOptionValue("v");
        commandBytes = new SwitchVideo(inputChStr, cli.getOptionValue("o")).getBytes();
        break;
      default:
        throw new UnsupportedOperationException("Operación no soportada");
    }
    // Ejecuta el comando y obtiene una respuesta
    serialPort.sendCommand(commandBytes, 0, commandBytes.length);
    byte[] response = new byte[1];
    serialPort.getResponse(response);
    if (response[0] == SerialPort.NACK)
      throw new RuntimeException("La operación ha fallado");
  }

  public static void main(String[] args) {
    new Cli().parse(args);
  }

}
