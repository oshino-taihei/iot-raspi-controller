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
    private static final String ATTRIBUTE_CPU_TEMP = "cpuTemp";
    private static final String PROVISIONING_FILE_PATH = "config/test-oshino-raspi-provisioning-file.conf";
    private static final String PROVISIONING_FILE_PASS = "lvp0xeTSMAOnC9Nx";
    private static boolean isRunning = false;
    private static double tooHotThreshold = 50.0;
    private static boolean useDummySensor = false;

    @RequestMapping("/")
    @ResponseBody
    String home() {
      def binding = ['tooHotThreshold': tooHotThreshold, 'useDummySensor': useDummySensor]
      render('home', binding)
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
        double cpuTemp = useDummySensor ? getDummyCpuTemp() : getCpuTemp()
        if(cpuTemp > tooHotThreshold) {
          logger.info("ALERT: attr=[${ATTRIBUTE_CPU_TEMP}],message=[${cpuTemp}]")
          AlertMessage alert = new AlertMessage.Builder()
            .format(URN_ALERT)
            .source(device.getEndpointId())
            .description("over ${tooHotThreshold}")
            .dataItem(ATTRIBUTE_CPU_TEMP, cpuTemp)
            .severity(AlertMessage.Severity.NORMAL)
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
    @RequestMapping("/settings")
    String settings(@RequestParam("cpu-temp") String cpuTemp,
                    @RequestParam(value = "use-dummy-sensor", required = false) String useDummySensor) {
      logger.info("UPDATE: cpu-temp:tooHotThreshold=[${cpuTemp}].")
      tooHotThreshold = cpuTemp as double
      if(!this.useDummySensor && useDummySensor == "1") {
        logger.info("UPDATE: use-dummy-sensor=[true].")
        this.useDummySensor = true
      } else if(this.useDummySensor && useDummySensor == null) {
        logger.info("UPDATE: use-dummy-sensor=[false].")
        this.useDummySensor = false
      }
      "redirect:/"
    }

    private double getCpuTemp() {
      "/opt/vc/bin/vcgencmd measure_temp".execute().text.find(/\d+\.\d+/) as double
    }

    private double getDummyCpuTemp() {
      // Gaussian(mean=50, variance=5^2)
    	Math.floor((50 + new Random().nextGaussian() * 5) * 10) / 10
    }

    private String render(String templateName, binding) {
      def f = new File("view/${templateName}.template")
      def engine = new groovy.text.SimpleTemplateEngine()
      def template = engine.createTemplate(f).make(binding)
      template.toString()
    }
}
