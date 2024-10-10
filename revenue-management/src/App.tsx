// src/App.tsx

import React, { useState } from 'react';

function App() {
  const [promotionId, setPromotionId] = useState('');
  const [actionTypeId, setActionTypeId] = useState('');
  const [parameters, setParameters] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    // Send data to backend
    const response = await fetch('/api/create-promotion', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ promotionId, actionTypeId, parameters }),
    });
    // Handle response
  };

  return (
    <form onSubmit={handleSubmit}>
      <input value={promotionId} onChange={(e) => setPromotionId(e.target.value)} placeholder="Promotion ID" />
      <input value={actionTypeId} onChange={(e) => setActionTypeId(e.target.value)} placeholder="Action Type ID" />
      <input value={parameters} onChange={(e) => setParameters(e.target.value)} placeholder="Parameters" />
      <button type="submit">Create Promotion</button>
    </form>
  );
}

export default App;
