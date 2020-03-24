docker run -d --name warp10 -p 8080:8080 -p 8081:8081 warp10io/warp10:latest
sleep 5
docker cp data/warp10/omsToken.mc2 warp10:/tmp/omsToken.mc2
docker exec -u warp10 -t warp10 bash -c 'java -cp /opt/warp10/bin/warp10-2.4.0.jar  io.warp10.worf.TokenGen /opt/warp10/etc/conf.d/*.conf /tmp/omsToken.mc2 /tmp/out.json'
docker cp warp10:/tmp/out.json data/warp10/token_oms.json
