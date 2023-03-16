# BK2048
![Top Language](https://img.shields.io/github/languages/top/bbayu123/bk2048-poc)
<a href="https://www.gnu.org/licenses/gpl-3.0.html">![GitHub](https://img.shields.io/github/license/bbayu123/bk2048-poc)</a>
<a href="https://www.spigotmc.org/resources/bkcommonlib.39590/">![BKCommonLib Version](https://img.shields.io/spiget/version/39590?label=BKCommonLib)</a>
<a href="https://github.com/bbayu123/bkminesweeper-poc">![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/bbayu123/bk2048-poc?label=BK2048)</a>
<a href="https://discord.gg/wvU2rFgSnw">![Discord](https://img.shields.io/discord/415909893233442826?label=Discord&logo=Discord)</a>

BK2048 is a SpigotMC plugin that lets players play 2048 on maps.

This plugin is an example of how to use the MapDisplay system in [BKCommonLib][1].

![Showcase Image](cover.png)

## Installation

Compile the plugin using [maven](https://maven.apache.org/).

Navigate to the root of the project, and type

```bash
mvn
```

This plugin requires [BKCommonLib][2] to function. You need to add BKCommonLib to your Spigot server 
before adding this plugin. 
This plugin should support all versions from version 1.13.2 onwards.


## Usage

To obtain a 2048 game, type

```
/2048 get
```

To control the map, either hold the map in your main hand, and use the regular movement keys 
(defaults are `W`, `A`, `S`, `D`, `Space`, `Left Shift`) to control it; or place the map in an 
item frame, and use regular interaction keys (defaults are `Left Click` and `Right Click`) to control it.

## Support

If you want more information on MapDisplays, head to the wiki page: <https://wiki.traincarts.net/p/Map_Display>

If you would like to see more examples, or want to ask for help when creating your own MapDisplay project,
reach out to us in the TeamBergerHealer discord server: <https://discord.gg/wvU2rFgSnw>

## License

This project uses the [GNU General Public License Version 3](https://www.gnu.org/licenses/gpl-3.0.html). 
For more information, see [LICENSE](LICENSE).

[1]: https://github.com/bergerkiller/BKCommonLib "Click to go to the GitHub page for BKCommonLib"
[2]: https://www.spigotmc.org/resources/bkcommonlib.39590/ "Click to go to the SpigotMC page for BKCommonLib"