# Remedy Ticketing Plugin

This plugin provides a ticketing iterface for BMC Remedy ITSM.

# Installation

1. Download the `opennms-remedy-ticketing-plugin.kar` file from [the latest release](https://github.com/OpenNMS/opennms-opa-plugin-ticketing-remedy/releases), and put it in your `$OPENNMS_HOME/deploy/` directory.
2. SSH into the Karaf shell:
   `ssh -p 8101 admin@localhost`
3. Load the Remedy ticketing feature, using the command:
   `feature:install remedy-ticketing`

# Configuration

Once loaded, the default configuration will be written to `$OPENNMS_HOME/etc/org.opennms.plugins.opa.ticketing.remedy.cfg`.
You may either edit the file directly, or use the `config:edit` command in Karaf:

```
config:edit org.opennms.plugins.opa.ticketing.remedy
config:property-set locale en_UK
config:update
```
