export default function App() {
  const nazwaSerwisu: string = 'Auto Serwis';
  const liczbaStanowisk: number = 3;

  return (
      <main>
        <h1>{nazwaSerwisu}</h1>
        <p>Liczba stanowisk: {liczbaStanowisk}</p>
        <button>Umów wizytę</button>
      </main>
  );
}