import { Tabs } from 'expo-router';
import React from 'react';
import { Platform } from 'react-native';

import { HapticTab } from '@/components/HapticTab';
import { IconSymbol } from '@/components/ui/IconSymbol';
import TabBarBackground from '@/components/ui/TabBarBackground';
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';

type TabBarIconProps = {
  color: string;
};

function ChatTabBarIcon({ color }: TabBarIconProps) {
  return <IconSymbol size={28} name="paperplane.fill" color={color} />;
}

function RememberTabBarIcon({ color }: TabBarIconProps) {
  return <IconSymbol size={28} name="magnifyingglass" color={color} />;
}

function HistoryTabBarIcon({ color }: TabBarIconProps) {
  return <IconSymbol size={28} name="clock.fill" color={color} />;
}


export default function TabLayout() {
  const colorScheme = useColorScheme();

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: Colors[colorScheme ?? 'light'].tint,
        headerShown: false,
        tabBarButton: HapticTab,
        tabBarBackground: TabBarBackground,
        tabBarStyle: Platform.select({
          ios: {
            position: 'absolute',
          },
          default: {},
        }),
      }}>
      <Tabs.Screen
        name="chat"
        options={{
          title: 'Chat',
          tabBarIcon: ChatTabBarIcon,
        }}
      />
      <Tabs.Screen
        name="recordatorios"
        options={{
          title: 'Recordatorios',
          tabBarIcon: RememberTabBarIcon,
        }}
      />
      <Tabs.Screen
        name="historial"
        options={{
          title: 'Historial',
          tabBarIcon: HistoryTabBarIcon,
        }}
      />
      <Tabs.Screen
        name="configuracion"
        options={{
          title: 'Configuracion',
          tabBarIcon: RememberTabBarIcon,
        }}
      />
    </Tabs>
  );
}
