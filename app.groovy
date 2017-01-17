import com.oracle.iot.client.*
import com.oracle.iot.client.device.*
import com.oracle.iot.client.message.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Controller
class IoTRaspberryPiManager {
    private static final Logger logger = LoggerFactory.getLogger(IoTRaspberryPiManager.class);
    private static final String URN_DEVICE = "urn:Accenture:oshinoraspi2";
    private static final String URN_ALERT = "urn:Accenture:oshinoraspi2:tooHotCpuAlert";
    private static final String ATTRIBUTE_CPU_TEMP = "cpu-temp";
    private static final String PROVISIONING_FILE_PATH = "config/test-oshino-raspi-provisioning-file.conf";
    private static final String PROVISIONING_FILE_PASS = "lvp0xeTSMAOnC9Nx";
    private static boolean isRunning = false;
    private static double tooHotThreshold = 50.0;

    @RequestMapping("/")
    @ResponseBody
    String home() {
      render('home')
    }
    @RequestMapping("/start")
    @ResponseBody
    String start() {
      logger.info("START: cpu-temp sensor.")
      DirectlyConnectedDevice device = new DirectlyConnectedDevice(PROVISIONING_FILE_PATH, PROVISIONING_FILE_PASS);
      if (!device.isActivated()) {
        device.activate(URN_DEVICE);
      }
      isRunning = true;
      while(isRunning) {
        double cpuTemp = getCpuTemp()
        if(cpuTemp > tooHotThreshold) {
          logger.info("ALERT: attr=[${ATTRIBUTE_CPU_TEMP}],message=[${cpuTemp}]")
          AlertMessage alert = new AlertMessage.Builder()
            .format(URN_ALERT)
            .source(device.getEndpointId())
            .description("over ${tooHotThreshold}")
            .dataItem(ATTRIBUTE_CPU_TEMP, cpuTemp)
            .severity(AlertMessage.Severity.CRITICAL)
            .build();
		      device.send(alert);
        } else {
          logger.info("MESSAGE: attr=[${ATTRIBUTE_CPU_TEMP}],message=[${cpuTemp}]")
          DataMessage message = new DataMessage.Builder()
    				.format("${URN_DEVICE}:attributes")
    				.source(device.getEndpointId())
    				.dataItem(ATTRIBUTE_CPU_TEMP, cpuTemp)
    				.build()
          device.send(message);
        }
        sleep(5000)
      }
      device.close()
      "finished"
    }
    @RequestMapping("/stop")
    @ResponseBody
    String stop() {
      logger.info("STOP: cpu-temp sensor.")
      isRunning = false
      "stopped successfully"
    }
    @RequestMapping("/cpu-temp/tooHotAlert")
    String cpuTempTooHotAlert(@RequestParam("cpu-temp") String cpuTemp) {
      logger.info("UPDATE: cpu-temp:tooHotThreshold=[${cpuTemp}].")
      tooHotThreshold = cpuTemp as double
      "redirect:/"
    }

    private double getCpuTemp() {
      "/opt/vc/bin/vcgencmd measure_temp".execute().text.find(/\d+\.\d+/) as double
    }
    private String render(String templateName) {
      def f = new File("view/${templateName}.template")
      def engine = new groovy.text.SimpleTemplateEngine()
      def binding = ['tooHotThreshold': tooHotThreshold]
      def template = engine.createTemplate(f).make(binding)
      template.toString()
    }
}
