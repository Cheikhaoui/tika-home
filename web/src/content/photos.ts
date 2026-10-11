// PLACEHOLDER PHOTOS from Unsplash (free to use, https://unsplash.com/license).
// To use the real photos: put them in /public/photos and replace `src` with "/photos/xxx.jpg"
// (set width/height to the real pixel size). Alt texts live in the dictionaries under gallery.alt.<id>.

export type Photo = {
  id: string;
  src: string;
  width: number;
  height: number;
  author: string;
  authorUrl: string;
};

const unsplash = (photoId: string) => `https://images.unsplash.com/photo-${photoId}`;
const author = (name: string, username: string) => ({
  author: name,
  authorUrl: `https://unsplash.com/@${username}`,
});

export const heroPhoto: Photo = {
  id: "courtyard",
  src: unsplash("1759250451654-72ab594ba2c9"),
  width: 4896,
  height: 3264,
  ...author("Gunnar Ridderström", "gunnarridder"),
};

export const galleryPhotos: Photo[] = [
  heroPhoto,
  { id: "poolDoor", src: unsplash("1750859479417-54383840d7e7"), width: 2600, height: 4000, ...author("Alexander Psiuk", "alexdeloy") },
  { id: "bedroom", src: unsplash("1517227355040-94989cb419f0"), width: 3280, height: 4928, ...author("Annie Spratt", "anniespratt") },
  { id: "fountain", src: unsplash("1539020140153-e479b8c22e70"), width: 4703, height: 3134, ...author("Niklas", "nelebki") },
  { id: "lounge", src: unsplash("1505576391880-b3f9d713dc4f"), width: 1870, height: 2805, ...author("Maria Orlova", "orlovamaria") },
  { id: "poolPalms", src: unsplash("1628642004970-1da51c8c7dec"), width: 2500, height: 2270, ...author("Jessica Kantak Bailey", "jkkantakbailey") },
  { id: "terrace", src: unsplash("1527534079274-21dc9147f3d0"), width: 2624, height: 3936, ...author("Toa Heftiba", "heftiba") },
  { id: "doorway", src: unsplash("1750859537685-24f071b0ae8f"), width: 2667, height: 4000, ...author("Alexander Psiuk", "alexdeloy") },
  { id: "living", src: unsplash("1505577058444-a3dab90d4253"), width: 1870, height: 2805, ...author("Maria Orlova", "orlovamaria") },
  { id: "poolCourtyard", src: unsplash("1761828122922-5a839ada5b76"), width: 4000, height: 6000, ...author("Benjamin Chambon", "benjamin_photo_lab") },
  { id: "garden", src: unsplash("1750859513748-90ad9625f1ae"), width: 3196, height: 4000, ...author("Alexander Psiuk", "alexdeloy") },
];

export const locationPhoto: Photo = {
  id: "koutoubia",
  src: unsplash("1597212618440-806262de4f6b"),
  width: 4512,
  height: 3000,
  ...author("Paul Macallan", "pemacallan"),
};
