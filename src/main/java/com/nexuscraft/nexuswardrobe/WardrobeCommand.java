package com.nexuscraft.nexuswardrobe;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /body (how are you doing) and /wardrobe (what can I wear). */
public final class WardrobeCommand implements CommandExecutor, TabCompleter {

    private final BodyService service;
    private final GarmentItems items;
    private Runnable reload = () -> { };

    public WardrobeCommand(BodyService service, GarmentItems items) {
        this.service = service;
        this.items = items;
    }

    public void setReload(Runnable reload) {
        this.reload = reload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("body")) {
            return body(sender, args);
        }
        return wardrobe(sender, args);
    }

    private boolean body(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("nexuswardrobe.admin")) {
                sender.sendMessage("§cYou can only look at your own body.");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cNo player called " + args[0] + " is online.");
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("§cUsage: /body <player>");
            return true;
        }
        if (service.exempt(target)) {
            sender.sendMessage("§7Body temperature does not apply to " + (target == sender ? "you" : target.getName())
                    + " right now (creative, spectator, or switched off in /path).");
            return true;
        }
        for (String line : report(target)) {
            sender.sendMessage(line);
        }
        return true;
    }

    /** The status screen. */
    List<String> report(Player target) {
        WardrobeConfig c = service.config();
        Body b = service.body(target.getUniqueId());
        HeatScanner.Result sources = new HeatScanner(c.params).scan(target, c.sourceRadius);
        Inputs in = service.inputsFor(target, sources);
        Thermo.Report r = Thermo.assess(c.params, b, in);
        Stage stage = b.stage();
        List<String> out = new ArrayList<>();
        out.add("§6[Body] §fCore " + stage.color() + c.core(b.core) + " §7(" + stage.color() + stage.label() + "§7)");
        String where = switch (in.dim()) {
            case NETHER -> "the Nether, " + c.degrees(c.params.netherF);
            case END -> "the End, " + c.degrees(c.params.endF);
            default -> "feels like " + c.degrees(in.feelsF()) + (in.sheltered() ? ", under a roof" : ", in the open")
                    + (in.precip().equals("NONE") ? "" : ", " + in.precip().toLowerCase(Locale.ROOT) + " falling");
        };
        out.add("§7Around you: §f" + where + " §8(" + service.climate().name() + ")");
        Insulation.Gear gear = in.gear();
        out.add("§7Your clothes: warmth §f" + Math.round(gear.insulation() * 100) + " §7(comfortable from §f"
                + c.degrees(Thermo.comfortableDownToF(c.params, gear)) + "§7 to §f" + c.degrees(Thermo.comfortableUpToF(c.params, gear)) + "§7 dry)"
                + (gear.cooling() > 0 ? ", cooling §f" + Math.round(gear.cooling()) : ""));
        StringBuilder more = new StringBuilder("§7");
        more.append("Wet §f").append(Math.round(b.wet * 100)).append('%');
        if (sources.heat() >= 1) {
            more.append(" §7| Fire nearby §6+").append(Math.round(sources.heat())).append("°");
        }
        if (sources.chill() >= 1) {
            more.append(" §7| Ice nearby §b-").append(Math.round(sources.chill())).append("°");
        }
        if (b.warmNow() > 0.5) {
            more.append(" §7| Warm food §6+").append(Math.round(b.warmNow())).append("° §7(").append(Math.round(b.warmLeft)).append("s)");
        }
        if (b.coolNow() > 0.5) {
            more.append(" §7| Cool treat §b-").append(Math.round(b.coolNow())).append("° §7(").append(Math.round(b.coolLeft)).append("s)");
        }
        out.add(more.toString());
        if (!service.mayHurt(target)) {
            out.add("§7Your NexusComfort setting means these only warn you: no slowness, no damage.");
        } else if (stage.coldSide() && stage.severity() > 0) {
            out.add(Texts.advice(stage, b.wet, in));
        } else if (stage.hotSide()) {
            out.add(Texts.advice(stage, b.wet, in));
        } else if (r.coldStress() > 0) {
            out.add("§7You are getting cold: dress warmer (§f/wardrobe§7) or find a fire before it sets in.");
        } else if (r.heatStress() > 0) {
            out.add("§7You are running warm: find shade or take off something heavy before it sets in.");
        }
        return out;
    }

    private boolean wardrobe(CommandSender sender, String[] args) {
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> {
                if (!sender.hasPermission("nexuswardrobe.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /wardrobe give <player> <garment>");
                    return true;
                }
                Player to = Bukkit.getPlayer(args[1]);
                Garment g = Garment.of(args[2]);
                if (to == null) {
                    sender.sendMessage("§cNo player called " + args[1] + " is online.");
                } else if (g == null) {
                    sender.sendMessage("§cUnknown garment. Try: " + String.join(", ", ids()));
                } else {
                    to.getInventory().addItem(items.create(g));
                    sender.sendMessage("§aGave " + to.getName() + " a " + g.title() + ".");
                }
            }
            case "reload" -> {
                if (!sender.hasPermission("nexuswardrobe.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                reload.run();
                sender.sendMessage("§aNexusWardrobe reloaded.");
            }
            default -> {
                sender.sendMessage("§6[Wardrobe] §7Clothes to craft. Wear them in the armour slots.");
                for (Garment g : Garment.values()) {
                    String stat = g.warmth() > 0.05 ? "warmth " + Math.round(g.warmth() * 100) : "cooling " + Math.round(g.cooling());
                    sender.sendMessage("§f" + g.title() + " §7(" + g.slot().name().toLowerCase(Locale.ROOT) + ", " + stat + "): " + GarmentItems.recipeText(g));
                }
                sender.sendMessage("§7Leather armour keeps a little warmth too; metal keeps almost none and bakes in the sun. §f/body§7 shows how you are doing.");
            }
        }
        return true;
    }

    static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (Garment g : Garment.values()) {
            out.add(g.id());
        }
        return out;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        boolean admin = sender.hasPermission("nexuswardrobe.admin");
        if (command.getName().equalsIgnoreCase("body")) {
            if (args.length == 1 && admin) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 1) {
            if (admin) {
                out.add("give");
                out.add("reload");
            }
            out.add("list");
        } else if (admin && args.length > 1 && args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    out.add(p.getName());
                }
            } else if (args.length == 3) {
                out.addAll(ids());
            }
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
