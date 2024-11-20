# Check if required arguments are provided
if [ -z "$1" ]; then
  echo "Error: The path to the root project is required as the first argument."
  exit 1
fi

if [ -z "$2" ]; then
  echo "Error: The signing key is required as the second argument."
  exit 1
fi

for i in EOCVSimPlugin/build/libs/*.jar; do
  java -classpath "$1/tools/Common.jar" io.github.deltacv.eocvsim.plugin.security.PluginSigningTool --plugin=$i --authority=deltacv --key=$2
done