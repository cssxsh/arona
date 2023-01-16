interface SubMenuItem {
  name: string;
  path: string;
}
interface MenuItem {
  name: string;
  children: SubMenuItem[];
}
const ConfigMenu: SubMenuItem[] = [
  {
    name: "条件执行",
    path: "/config/config-arona-blockly",
  },
];
const DatabaseMenu: SubMenuItem[] = [
  {
    name: "GachaPool",
    path: "/database/database-gacha-pool",
  },
  {
    name: "GachaHistory",
    path: "/database/database-gacha-history",
  },
];
const SettingMenu: SubMenuItem[] = [
  {
    name: "api",
    path: "/setting/setting-api",
  },
];
// eslint-disable-next-line import/prefer-default-export
export const MenuConfig: MenuItem[] = [
  {
    name: "side menu config",
    children: ConfigMenu,
  },
  {
    name: "side menu database",
    children: DatabaseMenu,
  },
  {
    name: "side menu setting",
    children: SettingMenu,
  },
];
