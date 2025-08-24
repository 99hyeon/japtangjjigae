import React from 'react';
import './App.css';

import {useEffect, useState} from "react";
import axios from "axios";

function App() {
  const [hello, setHello] = useState<string>("");

  useEffect(() => {
    axios.get<string>("/api/test").then((res) => {
      setHello(res.data);
    });
  }, []);

  return <div className="App">백엔드 데이터 : {hello}</div>;
}

export default App;
