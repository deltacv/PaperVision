for i in EOCVSimPlugin/build/libs/*.jar; do
  java -classpath "$1/tools/Common.jar" io.github.deltacv.eocvsim.plugin.security.PluginSigningTool --plugin=$i --authority=deltacv --key=$2
done