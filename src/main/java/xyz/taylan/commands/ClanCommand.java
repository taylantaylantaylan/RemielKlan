package xyz.taylan.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.kyori.adventure.audience.Audience;
import net.md_5.bungee.api.ChatColor;
import xyz.taylan.Clans;
import xyz.taylan.models.Clan;
import xyz.taylan.models.ClanInvite;
import xyz.taylan.utils.ClanInviteUtil;
import xyz.taylan.utils.ClansStorageUtil;
import xyz.taylan.utils.ColorUtils;

public class ClanCommand implements CommandExecutor {

	Logger logger = Clans.getPlugin().getLogger();

	private static final FileConfiguration messagesConfig = Clans.getPlugin().messagesFileManager.getMessagesConfig();
	private static final String CLAN_PLACEHOLDER = "%KLAN%";
	private static final String CLAN_PREFIX = "%KLANUNVAN%";
	private static final String CLAN_WAR = "%KAZANILANSAVAS%";
	private static final String INVITED_PLAYER = "%DAVETLI%";
	private static final String PLAYER_TO_KICK = "%ATILMISOYUNCU%";
	private static final String CLAN_OWNER = "%LIDER%";
	private static final String CLAN_MEMBER = "%UYE%";
	private static final String ALLY_CLAN = "%MUTTEFIKKLAN%";
	private static final String ALLY_OWNER = "%MUTTEFIKKLANSAHIBI%";
	private static final String TIME_LEFT = "%KALANZAMAN%";
	public HashMap<Player, Player> warmap = new HashMap<Player, Player>();
	public static HashMap<Player, Player> warmap2 = new HashMap<Player, Player>();
	HashMap<UUID, Long> homeCoolDownTimer = new HashMap<>();

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		FileConfiguration clansConfig = Clans.getPlugin().getConfig();
		if (sender instanceof Player) {
			Player player = ((Player) sender).getPlayer();
			if (args.length == 0) {
				sender.sendMessage(ColorUtils.translateColorCodes("&6Klan Komutlar?? Kullan??m??:&3" + "\n/klan kur <isim>"
						+ "\n/klan dagit" + "\n/klan davet <oyuncu>" + "\n/klan at <oyuncu>" + "\n/klan bilgi"
						+ "\n/klan liste" + "\n/klan unvan <unvan>" + "\n/klan muttef??k [ekle|sil] <klan-lideri>"
						+ "\n/klan [evayarla|ev]" + "\n/klan savas [baslat|kabulet] <klan-lideri>"));
			} else {

//----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("kur")) {
					if (args.length >= 2) {
						if (args[1].length() < 3) {
							player.sendMessage(
									ColorUtils.translateColorCodes(messagesConfig.getString("klan-ismi-cok-kisa")));
							return true;
						} else if (args[1].length() > 16) {
							player.sendMessage(
									ColorUtils.translateColorCodes(messagesConfig.getString("klan-ismi-cok-uzun")));
							return true;
						} else {
							StringBuilder stringBuilder = new StringBuilder();

							for (int i = 1; i < (args.length - 1); i++) {
								stringBuilder.append(args[i]).append(" ");
							}
							stringBuilder.append(args[args.length - 1]);

							if (!ClansStorageUtil.isClanExisting(player)) {
								ClansStorageUtil.createClan(player, args[1]);
								String clanCreated = ColorUtils
										.translateColorCodes(messagesConfig.getString("klan-basar??yla-kuruldu"))
										.replace(CLAN_PLACEHOLDER, args[1]);
								Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
								if (scoreboard.getTeam(args[1]) == null) {
									scoreboard.registerNewTeam(args[1]);
								}
								player.sendMessage(clanCreated);
							} else {
								String clanNotCreated = ColorUtils
										.translateColorCodes(messagesConfig.getString("klan-kurulamad??"))
										.replace(CLAN_PLACEHOLDER, args[1]);
								player.sendMessage(clanNotCreated);
							}
							return true;
						}
					}
				}

//----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("dagit")) {
					try {
						if (ClansStorageUtil.deleteClan(player)) {
							sender.sendMessage(ColorUtils
									.translateColorCodes(messagesConfig.getString("klan-basariyla-dagitildi")));
						} else {
							sender.sendMessage(
									ColorUtils.translateColorCodes(messagesConfig.getString("klan-dagitilamadi")));
						}
					} catch (IOException e) {
						sender.sendMessage(
								ColorUtils.translateColorCodes(messagesConfig.getString("klan-dagitilamadi")));
						e.printStackTrace();
					}
					return true;
				}
				// ----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("savas")) {
					if (args.length > 2) {
						if (args[1].equalsIgnoreCase("baslat")) {
							if (args[2].length() > 1) {
								if (args[1].length() < 1) {
									sender.sendMessage(ColorUtils.translateColorCodes(
											messagesConfig.getString("klan-savas-daveti-gecersiz-lider")));
									return true;
								}
								if (ClansStorageUtil.findClanByOwner(player) == null) {
									sender.sendMessage(ColorUtils
											.translateColorCodes(messagesConfig.getString("klan-savas-lider-degil")));
									return true;
								} else {
									String invitedPlayerStr = args[2];
									if (invitedPlayerStr.equalsIgnoreCase(player.getName())) {
										sender.sendMessage(ColorUtils.translateColorCodes(
												messagesConfig.getString("klan-savas-kendine-davet")));
									} else {
										Player invitedPlayer = Bukkit.getPlayer(invitedPlayerStr);
										if (invitedPlayer == null) {
											String playerNotFound = ColorUtils
													.translateColorCodes(
															messagesConfig.getString("klan-savas-davetlisi-bulunamad??"))
													.replace(INVITED_PLAYER, invitedPlayerStr);
											sender.sendMessage(playerNotFound);
										}
										if (invitedPlayer != null) {
											if (!(warmap.containsKey(invitedPlayer))) {
												warmap.put(invitedPlayer, player);
												new BukkitRunnable() {

													@Override
													public void run() {
														warmap.remove(invitedPlayer);
														String failureString = ColorUtils
																.translateColorCodes(messagesConfig
																		.getString("klan-savas-daveti-bitti"))
																.replace(INVITED_PLAYER, invitedPlayer.getName());
														if(!(warmap2.containsKey(player))) {
														invitedPlayer.sendMessage(failureString);
														player.sendMessage(failureString);
														}

													}
												}.runTaskLater(Clans.getPlugin(), 3000L);
												String confirmationString = ColorUtils
														.translateColorCodes(
																messagesConfig.getString("klan-savas-daveti-ba??ar??l??"))

														.replace(INVITED_PLAYER, invitedPlayer.getName());
												Scoreboard scoreboard = Bukkit.getScoreboardManager()
														.getMainScoreboard();
												Clan klan = ClansStorageUtil.findClanByPlayer(player);
												Clan klan2 = ClansStorageUtil.findClanByPlayer(invitedPlayer);
												if (scoreboard.getTeam(klan.getClanFinalName()) == null) {
													scoreboard.registerNewTeam(klan.getClanFinalName());
												}
												Team name = scoreboard.getTeam(klan.getClanFinalName());
												UUID ownerUUID = UUID.fromString(klan.getClanOwner());
												Player owner = Bukkit.getPlayer(ownerUUID);
												name.addEntry(owner.getName());
												ArrayList<String> members = klan.getClanMembers();
												for (String string : members) {
													UUID memberUUID = UUID.fromString(string);
													Player clanPlayer = Bukkit.getPlayer(memberUUID);
													name.addEntry(clanPlayer.getName());
												}
												if (scoreboard.getTeam(klan2.getClanFinalName()) == null) {
													scoreboard.registerNewTeam(klan2.getClanFinalName());
												}
												Team name2 = scoreboard.getTeam(klan2.getClanFinalName());
												UUID ownerUUID2 = UUID.fromString(klan2.getClanOwner());
												Player owner2 = Bukkit.getPlayer(ownerUUID2);
												name2.addEntry(owner2.getName());
												ArrayList<String> members2 = klan2.getClanMembers();
												for (String string2 : members2) {
													UUID memberUUID2 = UUID.fromString(string2);
													Player clanPlayer = Bukkit.getPlayer(memberUUID2);
													name2.addEntry(clanPlayer.getName());
												}
												player.sendMessage(confirmationString);
												String invitationString = ColorUtils
														.translateColorCodes(messagesConfig
																.getString("klan-savas-davetli-lider-bekleme"))
														.replace("%LIDER%", player.getName());
												invitedPlayer.sendMessage(invitationString);
											} else {
												String failureString = ColorUtils
														.translateColorCodes(
																messagesConfig.getString("klan-savas-daveti-ba??ar??s??z"))
														.replace(INVITED_PLAYER, invitedPlayer.getName());
												player.sendMessage(failureString);
											}
										}
									}
								}
							}
						} else if (args[1].equalsIgnoreCase("kabulet")) {
							if (warmap.containsKey(player)) {
								Clan klan = ClansStorageUtil.findClanByOwner(warmap.get(player));
								Clan klanwar = ClansStorageUtil.findClanByOwner(player);
								ArrayList<String> members = klan.getClanMembers();
								ArrayList<String> members2 = klanwar.getClanMembers();
								for (String string : members) {
									UUID memberUUID2 = UUID.fromString(string);
									Player clanPlayer = Bukkit.getPlayer(memberUUID2);
									
									if (clanPlayer != null) {
										clanPlayer.sendMessage(ColorUtils
												.translateColorCodes(messagesConfig.getString("savas-davet-kabul")));
										
									}

								}
								UUID owneruuid = UUID.fromString(klan.getClanOwner());
								Player clanowner = Bukkit.getPlayer(owneruuid);
								UUID owneruuid2 = UUID.fromString(klanwar.getClanOwner());
								Player clanowner2 = Bukkit.getPlayer(owneruuid2);
								clanowner.sendMessage(ColorUtils
										.translateColorCodes(messagesConfig.getString("savas-davet-kabul")));
								clanowner2.sendMessage(ColorUtils
										.translateColorCodes(messagesConfig.getString("savas-davet-kabul")));
								for (String string2 : members2) {
									UUID memberUUID2 = UUID.fromString(string2);
									Player clanPlayer = Bukkit.getPlayer(memberUUID2);
									
									if (clanPlayer !=null) {

										clanPlayer.sendMessage(ColorUtils
												.translateColorCodes(messagesConfig.getString("savas-davet-kabul")));
										
									}

								}
								new BukkitRunnable() {

									@Override
									public void run() {
										new BukkitRunnable() {
											
											@Override
											public void run() {
												UUID owneruuid2 = UUID.fromString(klanwar.getClanOwner());
												Player clanowner2 = Bukkit.getPlayer(owneruuid2);
												UUID owneruuid = UUID.fromString(klan.getClanOwner());
												Player clanowner = Bukkit.getPlayer(owneruuid);
												Location loc = new Location(Bukkit.getWorld("island"), 0, 75, -38);
												clanowner.teleport(loc);
												Location loc2 = new Location(Bukkit.getWorld("island"), -1, 75, 37);
												clanowner2.teleport(loc2);
												warmap2.put(clanowner, clanowner2);
												warmap2.put(clanowner2, clanowner);
												
											}
										}.runTaskLater(Clans.getPlugin(), clansConfig.getInt("klan-savas-hazirlik-suresi"));
										
										for (String string : members) {
											UUID memberUUID2 = UUID.fromString(string);
											
											Player clanPlayer = Bukkit.getPlayer(memberUUID2);
											if (clanPlayer !=null) {
												Location loc = new Location(Bukkit.getWorld("island"), 0, 75, -38);
												clanPlayer.teleport(loc);

												warmap2.put(clanPlayer, warmap.get(player));

											}
										}
										for (String string2 : members2) {
											UUID memberUUID2 = UUID.fromString(string2);
											Player clanPlayer = Bukkit.getPlayer(memberUUID2);
											if (clanPlayer !=null) {
												Location loc = new Location(Bukkit.getWorld("island"), -1, 75, 37);
												clanPlayer.teleport(loc);
												warmap2.put(clanPlayer, player);
											}
										}

									}
								}.runTaskLater(Clans.getPlugin(), clansConfig.getInt("klan-savas-hazirlik-suresi"));
							} else {
								player.sendMessage(ColorUtils
										.translateColorCodes(messagesConfig.getString("savas-davet-bulunamadi")));
							}
						} else {
							player.sendMessage(ColorUtils.translateColorCodes(
									messagesConfig.getString("dogru-olmayan-savas-komutu-kullan??m??")));
						}

					} else {
						player.sendMessage(ColorUtils
								.translateColorCodes(messagesConfig.getString("dogru-olmayan-savas-komutu-kullan??m??")));
					}
					return true;
				}

//----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("davet")) {
					if (args.length == 2) {
						if (args[1].length() < 1) {
							sender.sendMessage(ColorUtils
									.translateColorCodes(messagesConfig.getString("klan-daveti-gecersiz-oyuncu")));
							return true;
						}
						if (ClansStorageUtil.findClanByOwner(player) == null) {
							sender.sendMessage(ColorUtils
									.translateColorCodes(messagesConfig.getString("klan-daveti-lider-degil")));
							return true;
						} else {
							String invitedPlayerStr = args[1];
							if (invitedPlayerStr.equalsIgnoreCase(player.getName())) {
								sender.sendMessage(ColorUtils
										.translateColorCodes(messagesConfig.getString("klan-daveti-kendine-davet")));
							} else {
								Player invitedPlayer = Bukkit.getPlayer(invitedPlayerStr);
								if (invitedPlayer == null) {
									String playerNotFound = ColorUtils
											.translateColorCodes(messagesConfig.getString("klan-davetilisi-bulunamad??"))
											.replace(INVITED_PLAYER, invitedPlayerStr);
									sender.sendMessage(playerNotFound);
								} else if (ClansStorageUtil.findClanByPlayer(invitedPlayer) != null) {
									String playerAlreadyInClan = ColorUtils
											.translateColorCodes(
													messagesConfig.getString("zaten-klan??-olan-birine-davet"))
											.replace(INVITED_PLAYER, invitedPlayerStr);
									sender.sendMessage(playerAlreadyInClan);
								} else {
									Clan clan = ClansStorageUtil.findClanByOwner(player);
									if (clan.getClanMembers().size() >= clansConfig.getInt("max-klan-boyutu")) {
										Integer maxSize = clansConfig.getInt("max-klan-boyutu");
										player.sendMessage(ColorUtils
												.translateColorCodes(
														messagesConfig.getString("maksimum-klan-davetine-ulasildi"))
												.replace("%LIMIT%", maxSize.toString()));
										return true;
									}
									if (ClanInviteUtil.createInvite(player.getUniqueId().toString(),
											invitedPlayer.getUniqueId().toString()) != null) {
										String confirmationString = ColorUtils
												.translateColorCodes(messagesConfig.getString("klan-daveti-basarili"))
												.replace(INVITED_PLAYER, invitedPlayer.getName());
										player.sendMessage(confirmationString);
										String invitationString = ColorUtils
												.translateColorCodes(
														messagesConfig.getString("klan-davetli-oyuncu-bekleme"))
												.replace(CLAN_OWNER, player.getName());
										invitedPlayer.sendMessage(invitationString);
									} else {
										String failureString = ColorUtils
												.translateColorCodes(messagesConfig.getString("klan-daveti-basarisiz"))
												.replace(INVITED_PLAYER, invitedPlayer.getName());
										player.sendMessage(failureString);
									}
								}
							}
						}
					} else {
						sender.sendMessage(ColorUtils
								.translateColorCodes(messagesConfig.getString("klan-daveti-gecersiz-oyuncu")));
					}
					return true;
				}

//----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("unvan")) {
					if (args.length == 2) {
						if (ClansStorageUtil.isClanOwner(player)) {
							if (args[1].length() >= 3 && args[1].length() <= 16) {
								// C/U prefix
								Clan playerClan = ClansStorageUtil.findClanByOwner(player);
								ClansStorageUtil.updatePrefix(player, args[1]);
								String prefixConfirmation = ColorUtils
										.translateColorCodes(
												messagesConfig.getString("klan-unvan??-degistirme-ba??ar??l??"))
										.replace(CLAN_PREFIX, playerClan.getClanPrefix());
								sender.sendMessage(prefixConfirmation);
								return true;
							} else if (args[1].length() > 16) {
								sender.sendMessage(ColorUtils
										.translateColorCodes(messagesConfig.getString("klan-unvan??-cok-uzun")));
								return true;
							} else {
								sender.sendMessage(ColorUtils
										.translateColorCodes(messagesConfig.getString("klan-unvan??-cok-k??sa")));
								return true;
							}
						} else {
							sender.sendMessage(ColorUtils.translateColorCodes(
									messagesConfig.getString("unvan??-degistirmek-icin-lider-olman-laz??m")));
						}
					} else {
						sender.sendMessage(
								ColorUtils.translateColorCodes(messagesConfig.getString("gecersiz-unvan-ad??")));
					}
					return true;
				}

//----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("liste")) {
					Set<Map.Entry<UUID, Clan>> clans = ClansStorageUtil.getClans();
					StringBuilder clansString = new StringBuilder();
					if (clans.size() == 0) {
						sender.sendMessage(
								ColorUtils.translateColorCodes(messagesConfig.getString("listeleyecek-klan-yok")));
					} else {
						clansString.append(ColorUtils
								.translateColorCodes(messagesConfig.getString("klan-listesi-ba??l??????") + "\n"));
						clans.forEach((clan) -> clansString.append(
								ColorUtils.translateColorCodes(ChatColor.GOLD + clan.getValue().getClanFinalName()
										+ " &8[&c" + clan.getValue().getWarwin() + "&8]" + "\n")));
						clansString.append(" ");
						clansString.append(
								ColorUtils.translateColorCodes(messagesConfig.getString("klan-listesi-altbilgi")));
						sender.sendMessage(clansString.toString());
					}
					return true;
				}

//----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("kat??l")) {
					StringBuilder inviterUUIDString = new StringBuilder();
					Set<Map.Entry<UUID, ClanInvite>> clanInvitesList = ClanInviteUtil.getInvites();
					if (ClanInviteUtil.searchInvitee(player.getUniqueId().toString())) {
						clanInvitesList.forEach((invites) -> inviterUUIDString.append(invites.getValue().getInviter()));
						Clan clan = ClansStorageUtil
								.findClanByOwner(ClanInviteUtil.getInviteOwner(inviterUUIDString.toString()));
						if (clan != null) {
							if (ClansStorageUtil.addClanMember(clan, player)) {
								String joinMessage = ColorUtils
										.translateColorCodes(messagesConfig.getString("klana-kat??lma-basar??l??"))
										.replace(CLAN_PLACEHOLDER, clan.getClanFinalName());
								player.sendMessage(joinMessage);
							} else {
								String failureMessage = ColorUtils
										.translateColorCodes(messagesConfig.getString("klana-kat??lma-basar??s??z"))
										.replace(CLAN_PLACEHOLDER, clan.getClanFinalName());
								player.sendMessage(failureMessage);
							}
						} else {
							player.sendMessage(ColorUtils.translateColorCodes(
									messagesConfig.getString("klana-kat??l??m-basar??s??z-hatal??-klan")));
						}
					} else {
						player.sendMessage(ColorUtils
								.translateColorCodes(messagesConfig.getString("klana-kat??l??m-basar??s??z-davet-yok")));
					}
					return true;
				}

//----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("at")) {
					if (args.length == 2) {
						if (args[1].length() > 1) {
							Clan targetClan = ClansStorageUtil.findClanByOwner(player);
							if (ClansStorageUtil.findClanByOwner(player) != null) {
								Player playerToKick = Bukkit.getPlayer(args[1]);
								if (playerToKick != null) {
									if (!player.getName().equalsIgnoreCase(args[1])) {
										Clan playerClan = ClansStorageUtil.findClanByPlayer(playerToKick);
										if (targetClan.equals(playerClan)) {
											targetClan.removeClanMember(playerToKick.getUniqueId().toString());
											String playerKickedMessage = ColorUtils
													.translateColorCodes(messagesConfig.getString("klan-uyesi-at??ld??"))
													.replace(PLAYER_TO_KICK, args[1]);
											player.sendMessage(playerKickedMessage);
											if (playerToKick.isOnline()) {
												String kickMessage = ColorUtils
														.translateColorCodes(messagesConfig
																.getString("klandan-at??lan-oyuncuya-mesaj"))
														.replace(CLAN_PLACEHOLDER, targetClan.getClanFinalName());
												playerToKick.sendMessage(kickMessage);
												return true;
											}
										} else {
											String differentClanMessage = ColorUtils
													.translateColorCodes(messagesConfig
															.getString("secilmis-oyuncu-klan??n??zda-degil"))
													.replace(PLAYER_TO_KICK, args[1]);
											player.sendMessage(differentClanMessage);
										}
									} else {
										player.sendMessage(ColorUtils.translateColorCodes(
												messagesConfig.getString("kendinizi-atamazs??n??z")));
									}
								} else {
									String playerNotFound = ColorUtils
											.translateColorCodes(messagesConfig.getString("oyuncu-bulunamad??"))
											.replace(PLAYER_TO_KICK, args[1]);
									player.sendMessage(playerNotFound);
								}
							} else {
								player.sendMessage(ColorUtils.translateColorCodes(
										messagesConfig.getString("atmak-icin-lider-olman??z-gerek")));
							}
						} else {
							player.sendMessage(ColorUtils.translateColorCodes(
									messagesConfig.getString("dogru-olmayan-at-komutu-kullan??m??")));
						}
					} else {
						player.sendMessage(ColorUtils
								.translateColorCodes(messagesConfig.getString("dogru-olmayan-at-komutu-kullan??m??")));
					}
					return true;
				}

//----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("bilgi")) {
					Clan clanByOwner = ClansStorageUtil.findClanByOwner(player);
					Clan clanByPlayer = ClansStorageUtil.findClanByPlayer(player);
					if (clanByOwner != null) {
						ArrayList<String> clanMembers = clanByOwner.getClanMembers();
						ArrayList<String> clanAllies = clanByOwner.getClanAllies();
						StringBuilder clanInfo = new StringBuilder(ColorUtils
								.translateColorCodes(messagesConfig.getString("klan-bilgi-ba??l??????"))
								.replace(CLAN_PLACEHOLDER,
										ColorUtils.translateColorCodes(clanByOwner.getClanFinalName()))
								.replace(CLAN_PREFIX, ColorUtils.translateColorCodes(clanByOwner.getClanPrefix()))
								.replace(CLAN_WAR,
										ColorUtils.translateColorCodes(String.valueOf(clanByOwner.getWarwin()))));
						UUID clanOwnerUUID = UUID.fromString(clanByOwner.getClanOwner());
						Player clanOwner = Bukkit.getPlayer(clanOwnerUUID);
						if (clanOwner != null) {
							clanInfo.append(
									ColorUtils.translateColorCodes(messagesConfig.getString("klan-bilgi-lider-online"))
											.replace(CLAN_OWNER, clanOwner.getName()));
						} else {
							UUID uuid = UUID.fromString(clanByOwner.getClanOwner());
							String offlineOwner = Bukkit.getOfflinePlayer(uuid).getName();
							clanInfo.append(
									ColorUtils.translateColorCodes(messagesConfig.getString("klan-bilgi-lider-offline"))
											.replace(CLAN_OWNER, offlineOwner));
						}
						if (clanMembers.size() > 0) {
							clanInfo.append(ColorUtils
									.translateColorCodes(messagesConfig.getString("klan-bilgi-??yeler-ba??l??k")));
							for (String clanMember : clanMembers) {
								if (clanMember != null) {
									UUID memberUUID = UUID.fromString(clanMember);
									Player clanPlayer = Bukkit.getPlayer(memberUUID);
									if (clanPlayer != null) {
										clanInfo.append(ColorUtils
												.translateColorCodes(
														messagesConfig.getString("klan-bilgi-??yeler-online") + "\n")
												.replace(CLAN_MEMBER, clanPlayer.getName()));
									} else {
										UUID uuid = UUID.fromString(clanMember);
										String offlinePlayer = Bukkit.getOfflinePlayer(uuid).getName();
										clanInfo.append(ColorUtils
												.translateColorCodes(
														messagesConfig.getString("klan-bilgi-??yeler-offline") + "\n")
												.replace(CLAN_MEMBER, offlinePlayer));
									}
								}

							}
						}
						if (clanAllies.size() > 0) {
							clanInfo.append(" ");
							clanInfo.append(ColorUtils
									.translateColorCodes(messagesConfig.getString("klan-bilgi-muttefikler-ba??l??k")));
							for (String clanAlly : clanAllies) {
								if (clanAlly != null) {
									Player allyOwner = Bukkit.getPlayer(clanAlly);
									if (allyOwner != null) {
										Clan allyClan = ClansStorageUtil.findClanByOwner(allyOwner);
										String clanAllyName = allyClan.getClanFinalName();
										clanInfo.append(ColorUtils.translateColorCodes(messagesConfig
												.getString("klan-muttefik-uyeleri").replace(ALLY_CLAN, clanAllyName)));
									} else {
										UUID uuid = UUID.fromString(clanAlly);
										OfflinePlayer offlineOwnerPlayer = Bukkit.getOfflinePlayer(uuid);
										Clan offlineAllyClan = ClansStorageUtil
												.findClanByOfflineOwner(offlineOwnerPlayer);
										String offlineAllyName = offlineAllyClan.getClanFinalName();
										clanInfo.append(ColorUtils
												.translateColorCodes(messagesConfig.getString("klan-muttefik-uyeleri")
														.replace(ALLY_CLAN, offlineAllyName)));
									}
								}
							}
						}
						clanInfo.append(" ");
						if (ClansStorageUtil.isHomeSet(clanByOwner)) {
							clanInfo.append(
									ColorUtils.translateColorCodes(messagesConfig.getString("klan-evi-ayarl??")));
						} else {
							clanInfo.append(
									ColorUtils.translateColorCodes(messagesConfig.getString("klan-evi-belirlenmemis")));
						}
						clanInfo.append(" ");
						clanInfo.append(
								ColorUtils.translateColorCodes(messagesConfig.getString("klan-bilgi-altbilgi")));
						player.sendMessage(clanInfo.toString());

					} else if (clanByPlayer != null) {
						ArrayList<String> clanMembers = clanByPlayer.getClanMembers();
						ArrayList<String> clanAllies = clanByPlayer.getClanAllies();
						StringBuilder clanInfo = new StringBuilder(ColorUtils
								.translateColorCodes(messagesConfig.getString("klan-bilgi-ba??l??????"))
								.replace(CLAN_PLACEHOLDER,
										ColorUtils.translateColorCodes(clanByPlayer.getClanFinalName()))
								.replace(CLAN_PREFIX, ColorUtils.translateColorCodes(clanByPlayer.getClanPrefix())));
						UUID clanOwnerUUID = UUID.fromString(clanByPlayer.getClanOwner());
						Player clanOwner = Bukkit.getPlayer(clanOwnerUUID);
						if (clanOwner != null) {
							clanInfo.append(
									ColorUtils.translateColorCodes(messagesConfig.getString("klan-bilgi-lider-online"))
											.replace(CLAN_OWNER, clanOwner.getName()));
						} else {
							UUID uuid = UUID.fromString(clanByPlayer.getClanOwner());
							String offlineOwner = Bukkit.getOfflinePlayer(uuid).getName();
							clanInfo.append(
									ColorUtils.translateColorCodes(messagesConfig.getString("klan-bilgi-lider-offline"))
											.replace(CLAN_OWNER, offlineOwner));
						}
						if (clanMembers.size() > 0) {
							clanInfo.append(ColorUtils
									.translateColorCodes(messagesConfig.getString("klan-bilgi-??yeler-ba??l??k")));
							for (String clanMember : clanMembers) {
								if (clanMember != null) {
									UUID memberUUID = UUID.fromString(clanMember);
									Player clanPlayer = Bukkit.getPlayer(memberUUID);
									if (clanPlayer != null) {
										clanInfo.append(ColorUtils
												.translateColorCodes(
														messagesConfig.getString("klan-bilgi-??yeler-online") + "\n")
												.replace(CLAN_MEMBER, clanPlayer.getName()));
									} else {
										UUID uuid = UUID.fromString(clanMember);
										String offlinePlayer = Bukkit.getOfflinePlayer(uuid).getName();
										clanInfo.append(ColorUtils
												.translateColorCodes(
														messagesConfig.getString("klan-bilgi-??yeler-offline") + "\n")
												.replace(CLAN_MEMBER, offlinePlayer));
									}
								}

							}
						}
						if (clanAllies.size() > 0) {
							clanInfo.append(" ");
							clanInfo.append(ColorUtils
									.translateColorCodes(messagesConfig.getString("klan-bilgi-muttefikler-ba??l??k")));
							for (String clanAlly : clanAllies) {
								if (clanAlly != null) {
									Player allyOwner = Bukkit.getPlayer(clanAlly);
									if (allyOwner != null) {
										Clan allyClan = ClansStorageUtil.findClanByOwner(allyOwner);
										String clanAllyName = allyClan.getClanFinalName();
										clanInfo.append(ColorUtils.translateColorCodes(messagesConfig
												.getString("klan-muttefik-uyeleri").replace(ALLY_CLAN, clanAllyName)));
									} else {
										UUID uuid = UUID.fromString(clanAlly);
										OfflinePlayer offlineOwnerPlayer = Bukkit.getOfflinePlayer(uuid);
										Clan offlineAllyClan = ClansStorageUtil
												.findClanByOfflineOwner(offlineOwnerPlayer);
										String offlineAllyName = offlineAllyClan.getClanFinalName();
										clanInfo.append(ColorUtils
												.translateColorCodes(messagesConfig.getString("klan-muttefik-uyeleri")
														.replace(ALLY_CLAN, offlineAllyName)));
									}
								}
							}
						}
						clanInfo.append(" ");
						if (ClansStorageUtil.isHomeSet(clanByPlayer)) {
							clanInfo.append(
									ColorUtils.translateColorCodes(messagesConfig.getString("klan-evi-ayarl??")));
						} else {
							clanInfo.append(
									ColorUtils.translateColorCodes(messagesConfig.getString("klan-evi-belirlenmemis")));
						}
						clanInfo.append(" ");
						clanInfo.append(
								ColorUtils.translateColorCodes(messagesConfig.getString("klan-bilgi-altbilgi")));
						player.sendMessage(clanInfo.toString());
					} else {
						player.sendMessage(
								ColorUtils.translateColorCodes(messagesConfig.getString("bir-klanda-degil")));
					}
					return true;
				}

//----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("ayr??l")) {
					if (ClansStorageUtil.findClanByOwner(player) != null) {
						player.sendMessage(
								ColorUtils.translateColorCodes(messagesConfig.getString("klan-lideri-degil")));
						return true;
					}
					Clan targetClan = ClansStorageUtil.findClanByPlayer(player);
					if (targetClan != null) {
						if (targetClan.removeClanMember(player.getUniqueId().toString())) {
							String leaveMessage = ColorUtils
									.translateColorCodes(messagesConfig.getString("klan-ayrilma-basarili"))
									.replace(CLAN_PLACEHOLDER, targetClan.getClanFinalName());
							player.sendMessage(leaveMessage);
						} else {
							player.sendMessage(
									ColorUtils.translateColorCodes(messagesConfig.getString("klan-ayrilma-basarisiz")));
						}
					}
					return true;
				}

//----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("muttefik")) {
					if (args.length > 2) {
						if (args[1].equalsIgnoreCase("ekle")) {
							if (args[2].length() > 1) {
								if (ClansStorageUtil.isClanOwner(player)) {
									if (ClansStorageUtil.findClanByOwner(player) != null) {
										Player allyClanOwner = Bukkit.getPlayer(args[2]);
										if (allyClanOwner != null) {
											if (ClansStorageUtil.findClanByOwner(allyClanOwner) != null) {
												if (ClansStorageUtil.findClanByOwner(player) != ClansStorageUtil
														.findClanByOwner(allyClanOwner)) {
													if (ClansStorageUtil.findClanByOwner(player).getClanMembers()
															.size() >= clansConfig.getInt("max-klan-muttefikleri")) {
														Integer maxSize = clansConfig.getInt("max-klan-muttefikleri");
														player.sendMessage(ColorUtils
																.translateColorCodes(messagesConfig
																		.getString("klan-muttef??k-limitime-ulas??ld??"))
																.replace("%LIMIT%", maxSize.toString()));
														return true;
													}
													ClansStorageUtil.addClanAlly(player, allyClanOwner);
													Clan allyClan = ClansStorageUtil.findClanByOwner(allyClanOwner);
													player.sendMessage(ColorUtils.translateColorCodes(
															messagesConfig.getString("klan-muttef??klere-eklend??")
																	.replace(ALLY_CLAN, allyClan.getClanFinalName())));
													if (allyClanOwner.isOnline()) {
														allyClanOwner.sendMessage(
																ColorUtils.translateColorCodes(messagesConfig
																		.getString("klan-diger-muttef??klere-eklendi")
																		.replace(CLAN_OWNER, player.getName())));
													} else {
														player.sendMessage(ColorUtils.translateColorCodes(messagesConfig
																.getString("muttef??klere-ekleme-basarisiz")
																.replace(ALLY_OWNER, args[2])));
													}
												} else {
													player.sendMessage(ColorUtils.translateColorCodes(messagesConfig
															.getString("kendi-klanina-muttef??k-olamazsin")));
												}
											} else {
												player.sendMessage(ColorUtils.translateColorCodes(
														messagesConfig.getString("oyuncu-klan-lideri-degil")
																.replace(ALLY_OWNER, args[2])));
											}
										} else {
											player.sendMessage(ColorUtils.translateColorCodes(
													messagesConfig.getString("muttef??k-klan-lideri-offline")
															.replace(ALLY_OWNER, args[2])));
										}
									}
								} else {
									player.sendMessage(ColorUtils.translateColorCodes(
											messagesConfig.getString("klan-lideri-olman??z-gerek")));
								}
							} else {
								player.sendMessage(ColorUtils.translateColorCodes(
										messagesConfig.getString("dogru-olmayan-muttef??k-komudu-kullanimi")));
							}
							return true;
						} else if (args[1].equalsIgnoreCase("sil")) {
							if (args[2].length() > 1) {
								if (ClansStorageUtil.isClanOwner(player)) {
									if (ClansStorageUtil.findClanByOwner(player) != null) {
										Player allyClanOwner = Bukkit.getPlayer(args[2]);
										if (allyClanOwner != null) {
											if (ClansStorageUtil.findClanByOwner(allyClanOwner) != null) {
												Clan allyClan = ClansStorageUtil.findClanByOwner(allyClanOwner);
												List<String> alliedClans = ClansStorageUtil.findClanByOwner(player)
														.getClanAllies();
												if (alliedClans.contains(args[2])) {
													ClansStorageUtil.removeClanAlly(player, allyClanOwner);
													player.sendMessage(ColorUtils.translateColorCodes(
															messagesConfig.getString("klan-muttef??klerden-silindi")
																	.replace(ALLY_CLAN, allyClan.getClanFinalName())));
													if (allyClanOwner.isOnline()) {
														allyClanOwner.sendMessage(
																ColorUtils.translateColorCodes(messagesConfig
																		.getString("klan-diger-muttef??klerden-silindi")
																		.replace(CLAN_OWNER, player.getName())));
													}
												} else {
													player.sendMessage(ColorUtils.translateColorCodes(
															messagesConfig.getString("klan-muttef??klerden-silinemedi")
																	.replace(ALLY_OWNER, args[2])));
												}
											} else {
												player.sendMessage(ColorUtils.translateColorCodes(
														messagesConfig.getString("oyuncu-klan-lideri-degil")
																.replace(ALLY_OWNER, args[2])));
											}
										} else {
											player.sendMessage(ColorUtils.translateColorCodes(
													messagesConfig.getString("muttef??k-klan-sil-lider-offline")
															.replace(ALLY_OWNER, args[2])));
										}
									}
								} else {
									player.sendMessage(ColorUtils.translateColorCodes(
											messagesConfig.getString("klan-lideri-olman??z-gerek")));
								}
							} else {
								player.sendMessage(ColorUtils.translateColorCodes(
										messagesConfig.getString("dogru-olmayan-muttef??k-komudu-kullanimi")));
							}
						}
						return true;
					} else {
						player.sendMessage(ColorUtils.translateColorCodes(
								messagesConfig.getString("dogru-olmayan-muttef??k-komudu-kullanimi")));
					}
					return true;
				}
//----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("evayarla")) {
					if (clansConfig.getBoolean("klan-evi.aktif")) {
						if (ClansStorageUtil.isClanOwner(player)) {
							if (ClansStorageUtil.findClanByOwner(player) != null) {
								Clan clan = ClansStorageUtil.findClanByOwner(player);
								clan.setClanHomeWorld(player.getLocation().getWorld().getName());
								clan.setClanHomeX(player.getLocation().getX());
								clan.setClanHomeY(player.getLocation().getY());
								clan.setClanHomeZ(player.getLocation().getZ());
								clan.setClanHomeYaw(player.getLocation().getYaw());
								clan.setClanHomePitch(player.getLocation().getPitch());
								player.sendMessage(ColorUtils
										.translateColorCodes(messagesConfig.getString("klan-evi-basariyla-ayarlandi")));
							}
						} else {
							player.sendMessage(ColorUtils
									.translateColorCodes(messagesConfig.getString("klan-lideri-olman??z-gerek")));
						}
					} else {
						player.sendMessage(
								ColorUtils.translateColorCodes(messagesConfig.getString("??zellik-devred??s??")));
					}
					return true;
				}

//----------------------------------------------------------------------------------------------------------------------
				if (args[0].equalsIgnoreCase("ev")) {
					if (clansConfig.getBoolean("klan-evi.aktif")) {
						UUID uuid = player.getUniqueId();
						if (ClansStorageUtil.findClanByOwner(player) != null) {
							Clan clanByOwner = ClansStorageUtil.findClanByOwner(player);
							if (clanByOwner.getClanHomeWorld() != null) {
								World world = Bukkit.getWorld(clanByOwner.getClanHomeWorld());
								double x = clanByOwner.getClanHomeX();
								double y = clanByOwner.getClanHomeY() + 0.2;
								double z = clanByOwner.getClanHomeZ();
								float yaw = clanByOwner.getClanHomeYaw();
								float pitch = clanByOwner.getClanHomePitch();
								if (clansConfig.getBoolean("klan-evi.bekleme-s??resi.aktif")) {
									if (homeCoolDownTimer.containsKey(uuid)) {
										if (homeCoolDownTimer.get(uuid) > System.currentTimeMillis()) {
											Long timeLeft = (homeCoolDownTimer.get(uuid) - System.currentTimeMillis())
													/ 1000;
											player.sendMessage(ColorUtils.translateColorCodes(
													messagesConfig.getString("bekleme-s??resi-bekleme")
															.replace(TIME_LEFT, timeLeft.toString())));
										} else {
											homeCoolDownTimer.put(uuid, System.currentTimeMillis()
													+ (clansConfig.getLong("klan-evi.bekleme-s??resi.s??re") * 1000));
											Location location = new Location(world, x, y, z, yaw, pitch);
											player.teleport(location);
											player.sendMessage(ColorUtils.translateColorCodes(
													messagesConfig.getString("basar??yla-eve-??s??nlan??ld??")));
										}
									} else {
										homeCoolDownTimer.put(uuid, System.currentTimeMillis()
												+ (clansConfig.getLong("klan-evi.bekleme-s??resi.s??re") * 1000));
										Location location = new Location(world, x, y, z, yaw, pitch);
										player.teleport(location);
										player.sendMessage(ColorUtils.translateColorCodes(
												messagesConfig.getString("basar??yla-eve-??s??nlan??ld??")));
									}
								} else {
									Location location = new Location(world, x, y, z, yaw, pitch);
									player.teleport(location);
									player.sendMessage(ColorUtils.translateColorCodes(
											messagesConfig.getString("basar??yla-eve-??s??nlan??ld??")));
								}
							} else {
								player.sendMessage(
										ColorUtils.translateColorCodes(messagesConfig.getString("ev-ayarlanmamis")));
							}
						} else if (ClansStorageUtil.findClanByPlayer(player) != null) {
							Clan clanByPlayer = ClansStorageUtil.findClanByPlayer(player);
							if (clanByPlayer.getClanHomeWorld() != null) {
								World world = Bukkit.getWorld(clanByPlayer.getClanHomeWorld());
								double x = clanByPlayer.getClanHomeX();
								double y = clanByPlayer.getClanHomeY() + 0.2;
								double z = clanByPlayer.getClanHomeZ();
								float yaw = clanByPlayer.getClanHomeYaw();
								float pitch = clanByPlayer.getClanHomePitch();
								if (clansConfig.getBoolean("klan-evi.bekleme-s??resi.aktif")) {
									if (homeCoolDownTimer.containsKey(uuid)) {
										if (homeCoolDownTimer.get(uuid) > System.currentTimeMillis()) {
											Long timeLeft = (homeCoolDownTimer.get(uuid) - System.currentTimeMillis())
													/ 1000;
											player.sendMessage(ColorUtils.translateColorCodes(
													messagesConfig.getString("bekleme-s??resi-bekleme")
															.replace(TIME_LEFT, timeLeft.toString())));
										} else {
											homeCoolDownTimer.put(uuid, System.currentTimeMillis()
													+ (clansConfig.getLong("klan-evi.bekleme-s??resi.s??re") * 1000));
											Location location = new Location(world, x, y, z, yaw, pitch);
											player.teleport(location);
											player.sendMessage(ColorUtils.translateColorCodes(
													messagesConfig.getString("basar??yla-eve-??s??nlan??ld??")));
										}
									} else {
										homeCoolDownTimer.put(uuid, System.currentTimeMillis()
												+ (clansConfig.getLong("klan-evi.bekleme-s??resi.s??re") * 1000));
										Location location = new Location(world, x, y, z, yaw, pitch);
										player.teleport(location);
										player.sendMessage(ColorUtils.translateColorCodes(
												messagesConfig.getString("basar??yla-eve-??s??nlan??ld??")));
									}
								} else {
									Location location = new Location(world, x, y, z, yaw, pitch);
									player.teleport(location);
									player.sendMessage(ColorUtils.translateColorCodes(
											messagesConfig.getString("basar??yla-eve-??s??nlan??ld??")));
								}
							} else {
								player.sendMessage(
										ColorUtils.translateColorCodes(messagesConfig.getString("ev-ayarlanmamis")));
							}
						} else {
							player.sendMessage(ColorUtils.translateColorCodes(
									messagesConfig.getString("??s??nlama-basarisiz-bir-klanda-degil")));
						}
					} else {
						player.sendMessage(
								ColorUtils.translateColorCodes(messagesConfig.getString("??zellik-devred??s??")));
					}
					return true;
				}

//----------------------------------------------------------------------------------------------------------------------
				else {
					player.sendMessage(
							ColorUtils.translateColorCodes(messagesConfig.getString("dogru-olmayan-komut-kullan??m??")));
				}
			}
		}
//----------------------------------------------------------------------------------------------------------------------
		if (sender instanceof ConsoleCommandSender)

		{
			logger.warning(ColorUtils.translateColorCodes(messagesConfig.getString("sadece-oyuncu")));
		}
		// If the player (or console) uses our command correct, we can return true
		return true;
	}
}
