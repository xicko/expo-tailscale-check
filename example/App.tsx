import ExpoTailscaleCheck, { useTailscaleState } from 'expo-tailscale-check';
import { SafeAreaView, ScrollView, Text, View } from 'react-native';

export default function App() {
  const state = useTailscaleState();
  const iface = ExpoTailscaleCheck.tailscaleInterface();

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.container}>
        <Text style={styles.header}>Tailscale Check</Text>
        <Group name="State">
          <Text style={styles.value}>{state}</Text>
        </Group>
        <Group name="Interface">
          <Text style={styles.value}>{iface ? `${iface.name} — ${iface.ip}` : 'none'}</Text>
        </Group>
      </ScrollView>
    </SafeAreaView>
  );
}

function Group(props: { name: string; children: React.ReactNode }) {
  return (
    <View style={styles.group}>
      <Text style={styles.groupHeader}>{props.name}</Text>
      {props.children}
    </View>
  );
}

const styles = {
  header: { fontSize: 30, margin: 20 },
  groupHeader: { fontSize: 20, marginBottom: 12 },
  value: { fontSize: 16 },
  group: { margin: 20, backgroundColor: '#fff', borderRadius: 10, padding: 20 },
  container: { flex: 1, backgroundColor: '#eee' },
};
