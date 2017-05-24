## OneOps Packer 

[packer.io](https://www.packer.io/) + [OneOps Build](https://github.com/oneops/oneops-build-converter) = OneOps Single Stand Alone Instance

This project required [packer.io](https://www.packer.io) therefore it must be installed on machine that is going to be running this tool.

### packer.io

Packer is easy to use and automates the creation of any type of machine image. It embraces modern configuration management by encouraging you to use automated scripts to install and configure the software within your Packer-made images. Packer brings machine images into the modern age, unlocking untapped potential and opening new opportunities.

#### [Install Packer](https://www.packer.io/intro/getting-started/install.html)

Homebrew
If you're using OS X and [Homebrew](https://brew.sh), you can install Packer by running:
```
$ brew install packer
```
Chocolatey
If you're using Windows and [Chocolatey](http://chocolatey.org/), you can install Packer by running:
```
choco install packer
```
### So how do I run this thing?

After you clone this repo you can start packing things by running:
```
sh build-oneops.sh
```
Yes that's all to it!  If everything works [packer](https://packer.io) will output a box file that can be imported into Vagrant.

This tool depends on [OneOps Build](https://github.com/oneops/oneops-build-converter) of which you can get most up-to-date by running:

```
sh build-oneops.sh -f
```

This will clean up everything and pull the latest [OneOps Build](https://github.com/oneops/oneops-build-converter) into the workspace.
