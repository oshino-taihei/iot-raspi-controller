import com.oracle.iot.client.*
import com.oracle.iot.client.device.*
import com.oracle.iot.client.message.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RestController
class IoTRaspberryPiManager {
    private static final String URN_DEVICE = "urn:Accenture:oshinoraspi2";
    private static final String ATTRIBUTE_CPU_TEMP = "cpu-temp";
    private static final String PROVISIONING_FILE_PATH = "config/test-oshino-raspi-provisioning-file.conf";
    private static final String PROVISIONING_FILE_PASS = "lvp0xeTSMAOnC9Nx";

    private static boolean isRunning = false;
    private static final Logger logger = LoggerFactory.getLogger(IoTRaspberryPiManager.class);

    @RequestMapping("/")
    String home() {
      render('home')
    }
    @RequestMapping("/start")
    String start() {
      logger.info("START: cpu-temp sensor.")
      DirectlyConnectedDevice device = new DirectlyConnectedDevice(PROVISIONING_FILE_PATH, PROVISIONING_FILE_PASS);
      if (!device.isActivated()) {
        device.activate(URN_DEVICE);
      }
      isRunning = true;
      while(isRunning) {
        String message = getCpuTemp()
        logger.info("attr=[${ATTRIBUTE_CPU_TEMP}],message=[${message}]")
        DataMessage dataMessage = new DataMessage.Builder()
  				.format(URN_DEVICE + ":attributes")
  				.source(device.getEndpointId())
  				.dataItem(ATTRIBUTE_CPU_TEMP, message)
  				.build();
		    device.send(dataMessage);
        sleep(5000)
      }
      device.close()
    }
    @RequestMapping("/stop")
    String stop() {
      logger.info("STOP: cpu-temp sensor.")
      isRunning = false
      "stopped successfully"
    }

    private String getCpuTemp() {
      "/opt/vc/bin/vcgencmd measure_temp".execute().text.find(/\d+\.\d+/)
    }
    private String render(String templateName, binding = ['sample': 'OK']) {
      def f = new File("view/${templateName}.template")
      def engine = new groovy.text.SimpleTemplateEngine()
      def template = engine.createTemplate(f).make(binding)
      template.toString()
    }
}
