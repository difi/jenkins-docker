@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml

File configFile = this.args[0] as File
Map config = new Yaml().load(configFile.text)
List<String> knownHosts = config.ssh.knownHosts
println "Creating ~/.ssh/known_hosts..."
File outputFile = new File('~/.ssh/known_hosts')
outputFile.delete()
outputFile << knownHosts.join('\n')
