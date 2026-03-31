const board = document.querySelector('.grid-board');

for (let i = 0; i<81; i++)
{
    const square = document.createElement('div')
    square.className = 'square';
    let row = String.fromCharCode(97 + Math.floor(i / 9)); 
    let col = i % 9; 
    if((Math.floor(i / 9) + col) % 2 == 0) 
    {   
        square.classList.add('light-square');
    }
    else
    {
        square.classList.add('dark-square');
    }
    square.id = row + col;
    board.appendChild(square);

}


