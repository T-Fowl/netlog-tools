# NetLog Tools

A tool for reading chromium netlog files to either extract network responses
or convert network requests into more
usable formats.

A few similar solutions exist, most notably a Fiddler plugin for loading
netlog files. However, these tools were either too manual, or simply didn't fit my needs.

### Development

This tool is provided as-is. Some development might happen occasionally
however no guarantees are made.

### Usage

Note that here `netlog` is an alias for `java -jar <released jar file>.jar`

```netlog -h```

```
Usage: netlog [<options>] <command> [<args>]...

Options:
  -h, --help  Show this message and exit

Commands:
  convert
  extract
```

```netlog convert -h```
```
Usage: netlog convert [<options>] [<inputs>]...

Filtering Options:
  --ignore-google-requests  Filter out google http transactions. Uses the
                            following regex: google|gstatic|googleapis

Options:
  -o, --output=<file>      Output file
  -f, --format=(JSON|HAR)  Output file format
  -h, --help               Show this message and exit

Arguments:
  <inputs>  Netlog files
```

```netlog extract -h```
```
Usage: netlog extract [<options>] [<inputs>]...

Filtering Options:
  --ignore-google-requests  Filter out google http transactions. Uses the
                            following regex: google|gstatic|googleapis

Options:
  -o, --output=<path>  Directory to extract responses within
  -h, --help           Show this message and exit

Arguments:
  <inputs>  Netlog files
```

### Installation

Released as a single jar

### Building

Requirements: JDK

`gradle build shadowJar`

Then find the result in build/libs/netlog-*-all.jar