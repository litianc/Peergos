package peergos.server.net;

import com.sun.net.httpserver.*;
import peergos.server.util.*;
import peergos.server.util.Logging;
import peergos.shared.cbor.*;
import peergos.shared.crypto.hash.*;
import peergos.shared.storage.*;
import peergos.shared.util.*;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;

/** This is the http endpoint for SpaceUsage calls
 *
 */
public class StorageHandler implements HttpHandler {
    private static final Logger LOG = Logging.LOG();

    private final SpaceUsage spaceUsage;

    public StorageHandler(SpaceUsage spaceUsage) {
        this.spaceUsage = spaceUsage;
    }

    public void handle(HttpExchange exchange)
    {
        long t1 = System.currentTimeMillis();
        DataInputStream din = new DataInputStream(exchange.getRequestBody());


        String path = exchange.getRequestURI().getPath();
        if (path.startsWith("/"))
            path = path.substring(1);
        String[] subComponents = path.substring(Constants.SPACE_USAGE_URL.length()).split("/");
        String method = subComponents[0];

        Map<String, List<String>> params = HttpUtil.parseQuery(exchange.getRequestURI().getQuery());
        Function<String, String> last = key -> params.get(key).get(params.get(key).size() - 1);
        PublicKeyHash owner = PublicKeyHash.fromString(params.get("owner").get(0));

        Cborable result;
        try {
            switch (method) {
                case "usage":
                    long usage = spaceUsage.getUsage(owner).join();
                    result = new CborObject.CborLong(usage);
                    break;
                case "quota":
                    byte[] signedTime = ArrayOps.hexToBytes(last.apply("auth"));
                    long quota = spaceUsage.getQuota(owner, signedTime).join();
                    result = new CborObject.CborLong(quota);
                    break;
                case "request":
                    byte[] signedReq = ArrayOps.hexToBytes(last.apply("req"));
                    boolean res = spaceUsage.requestSpace(owner, signedReq).join();
                    result = new CborObject.CborBoolean(res);
                    break;
                default:
                    throw new IOException("Unknown method in StorageHandler!");
            }

            byte[] b = result.serialize();
            exchange.sendResponseHeaders(200, b.length);
            exchange.getResponseBody().write(b);
        } catch (Exception e) {
            HttpUtil.replyError(exchange, e);
        } finally {
            exchange.close();
            long t2 = System.currentTimeMillis();
            LOG.info("SpaceUsage handled " + method + " request in: " + (t2 - t1) + " mS");
        }
    }
}
