# pip install mitmproxy

# copy mitm certs into JAVA cacerts
# keytool -importcert -alias mitmproxy -file mitmproxy.pem -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit

mitmproxy -p 8888